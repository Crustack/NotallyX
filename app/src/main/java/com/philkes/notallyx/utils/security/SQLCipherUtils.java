/*
 * Copyright (c) 2012-2017 CommonsWare, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.philkes.notallyx.utils.security;

import android.content.Context;
import android.text.Editable;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteStatement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class SQLCipherUtils {
    /**
     * The detected state of the database, based on whether we can open it
     * without a passphrase.
     */
    public enum State {
        DOES_NOT_EXIST, UNENCRYPTED, ENCRYPTED,
        /**
         * The file exists but cannot be opened at all, e.g. because it is corrupted or truncated.
         * Such a file must not be treated as {@link #ENCRYPTED}, otherwise the app tries to decrypt
         * a plaintext database and turns a recoverable file into an unopenable one.
         */
        UNREADABLE
    }

    /**
     * Suffixes of the files SQLite maintains next to the actual database file. They belong to exactly
     * one database file, so they have to be removed whenever that file is replaced.
     */
    private static final String[] COMPANION_SUFFIXES = {"-wal", "-shm", "-journal"};

    private static void deleteCompanionFiles(File databaseFile) {
        File directory = databaseFile.getParentFile();
        if (directory == null) {
            return;
        }
        for (String suffix : COMPANION_SUFFIXES) {
            new File(directory, databaseFile.getName() + suffix).delete();
        }
    }

    /**
     * Replaces {@code target} with {@code replacement} by renaming, keeping the previous file until
     * the rename succeeded so it can be restored, and removing the stale {@code -wal}/{@code -shm}
     * files of the replaced database.
     */
    private static void replaceDatabaseFile(File replacement, File target) throws IOException {
        File rollback = new File(target.getParentFile(), target.getName() + ".rollback");
        rollback.delete();
        boolean targetExisted = target.exists();
        if (targetExisted && !target.renameTo(rollback)) {
            throw new IOException("Failed to move '" + target.getAbsolutePath() + "' aside");
        }
        deleteCompanionFiles(replacement);
        if (!replacement.renameTo(target)) {
            if (targetExisted) {
                rollback.renameTo(target);
            }
            throw new IOException("Failed to move '" + replacement.getAbsolutePath()
                    + "' to '" + target.getAbsolutePath() + "'");
        }
        deleteCompanionFiles(target);
        rollback.delete();
    }

    private static boolean isCorruptionError(Exception e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message == null) {
                continue;
            }
            String lowerCase = message.toLowerCase();
            if (lowerCase.contains("malformed") || lowerCase.contains("corrupt")
                    || lowerCase.contains("disk i/o error")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine whether or not this database appears to be encrypted, based
     * on whether we can open it without a passphrase.
     *
     * @param ctxt   a Context
     * @param dbName the name of the database, as used with Room, SQLiteOpenHelper,
     *               etc.
     * @return the detected state of the database
     */
    public static State getDatabaseState(Context ctxt, String dbName) {
        System.loadLibrary("sqlcipher");
        return (getDatabaseState(ctxt.getDatabasePath(dbName)));
    }

    /**
     * Determine whether or not this database appears to be encrypted, based
     * on whether we can open it without a passphrase.
     * <p>
     * NOTE: You are responsible for ensuring that net.sqlcipher.database.SQLiteDatabase.loadLibs()
     * is called before calling this method. This is handled automatically with the
     * getDatabaseState() method that takes a Context as a parameter.
     *
     * @param dbPath a File pointing to the database
     * @return the detected state of the database
     */
    public static State getDatabaseState(File dbPath) {
        if (dbPath.exists()) {
            SQLiteDatabase db = null;

            try {
                db = SQLiteDatabase.openDatabase(dbPath.getAbsolutePath(), "", null,
                        SQLiteDatabase.OPEN_READONLY, null, null);
                db.getVersion();

                return (State.UNENCRYPTED);
            } catch (Exception e) {
                return (isCorruptionError(e) ? State.UNREADABLE : State.ENCRYPTED);
            } finally {
                if (db != null) {
                    db.close();
                }
            }
        }

        return (State.DOES_NOT_EXIST);
    }

    /**
     * Replaces this database with a version encrypted with the supplied
     * passphrase, deleting the original. Do not call this while the database
     * is open, which includes during any Room migrations.
     * <p>
     * The passphrase is untouched in this call. If you are going to turn around
     * and use it with SafeHelperFactory.fromUser(), fromUser() will clear the
     * passphrase. If not, please set all bytes of the passphrase to 0 or something
     * to clear out the passphrase.
     *
     * @param ctxt   a Context
     * @param dbName the name of the database, as used with Room, SQLiteOpenHelper,
     *               etc.
     * @param editor the passphrase, such as obtained by calling getText() on an
     *               EditText
     * @throws IOException
     */
    public static void encrypt(Context ctxt, String dbName, Editable editor)
            throws IOException {
        char[] passphrase = new char[editor.length()];

        editor.getChars(0, editor.length(), passphrase, 0);
        encrypt(ctxt, dbName, passphrase);
    }

    /**
     * Replaces this database with a version encrypted with the supplied
     * passphrase, deleting the original. Do not call this while the database
     * is open, which includes during any Room migrations.
     * <p>
     * The passphrase is untouched in this call. If you are going to turn around
     * and use it with SafeHelperFactory.fromUser(), fromUser() will clear the
     * passphrase. If not, please set all bytes of the passphrase to 0 or something
     * to clear out the passphrase.
     *
     * @param ctxt       a Context
     * @param dbName     the name of the database, as used with Room, SQLiteOpenHelper,
     *                   etc.
     * @param passphrase the passphrase from the user
     * @throws IOException
     */
    public static void encrypt(Context ctxt, String dbName, char[] passphrase)
            throws IOException {
        encrypt(ctxt, ctxt.getDatabasePath(dbName), getBytes(passphrase));
    }

    /**
     * Replaces this database with a version encrypted with the supplied
     * passphrase, deleting the original. Do not call this while the database
     * is open, which includes during any Room migrations.
     * <p>
     * The passphrase is untouched in this call. If you are going to turn around
     * and use it with SafeHelperFactory.fromUser(), fromUser() will clear the
     * passphrase. If not, please set all bytes of the passphrase to 0 or something
     * to clear out the passphrase.
     *
     * @param ctxt       a Context
     * @param dbName     the name of the database, as used with Room, SQLiteOpenHelper,
     *                   etc.
     * @param passphrase the passphrase
     * @throws IOException
     */
    public static void encrypt(Context ctxt, String dbName, byte[] passphrase)
            throws IOException {
        encrypt(ctxt, ctxt.getDatabasePath(dbName), passphrase);
    }

    /**
     * Replaces this database with a version encrypted with the supplied
     * passphrase, deleting the original. Do not call this while the database
     * is open, which includes during any Room migrations.
     * <p>
     * The passphrase is untouched in this call. If you are going to turn around
     * and use it with SafeHelperFactory.fromUser(), fromUser() will clear the
     * passphrase. If not, please set all bytes of the passphrase to 0 or something
     * to clear out the passphrase.
     *
     * @param ctxt         a Context
     * @param originalFile a File pointing to the database
     * @param passphrase   the passphrase from the user
     * @throws IOException
     */
    public static void encrypt(Context ctxt, File originalFile, char[] passphrase)
            throws IOException {
        encrypt(ctxt, originalFile, getBytes(passphrase));
    }

    /**
     * Replaces this database with a version encrypted with the supplied
     * passphrase, deleting the original. Do not call this while the database
     * is open, which includes during any Room migrations.
     * <p>
     * The passphrase is untouched in this call. If you are going to turn around
     * and use it with SafeHelperFactory.fromUser(), fromUser() will clear the
     * passphrase. If not, please set all bytes of the passphrase to 0 or something
     * to clear out the passphrase.
     *
     * @param ctxt         a Context
     * @param originalFile a File pointing to the database
     * @param passphrase   the passphrase from the user
     * @throws IOException
     */
    public static void encrypt(Context ctxt, File originalFile, byte[] passphrase)
            throws IOException {
        System.loadLibrary("sqlcipher");

        if (originalFile.exists()) {
            File newFile = File.createTempFile("sqlcipherutils", "tmp",
                    originalFile.getParentFile());
            SQLiteDatabase db = SQLiteDatabase.openDatabase(originalFile.getAbsolutePath(), 
                    "", null, SQLiteDatabase.OPEN_READWRITE, null, 
                    null);
            int version = db.getVersion();

            db.close();

            db = SQLiteDatabase.openDatabase(newFile.getAbsolutePath(), passphrase,
                    null, SQLiteDatabase.OPEN_READWRITE, null, null);

            final SQLiteStatement st = db.compileStatement("ATTACH DATABASE ? AS plaintext KEY ''");

            st.bindString(1, originalFile.getAbsolutePath());
            st.execute();

            db.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')");
            db.rawExecSQL("DETACH DATABASE plaintext");
            db.setVersion(version);
            st.close();
            db.close();

            replaceDatabaseFile(newFile, originalFile);
        } else {
            throw new FileNotFoundException(originalFile.getAbsolutePath() + " not found");
        }
    }

    /**
     * Replaces this database with a decrypted version, deleting the original
     * encrypted database. Do not call this while the database is open, which
     * includes during any Room migrations.
     * <p>
     * The passphrase is untouched in this call. Please set all bytes of the
     * passphrase to 0 or something to clear out the passphrase if you are done
     * with it.
     *
     * @param ctxt         a Context
     * @param originalFile a File pointing to the encrypted database
     * @param passphrase   the passphrase from the user for the encrypted database
     * @throws IOException
     */
    public static void decrypt(Context ctxt, File originalFile, char[] passphrase)
            throws IOException {
        decrypt(ctxt, originalFile, getBytes(passphrase));
    }

    public static void decrypt(Context ctxt, String dbName, byte[] passphrase) throws IOException {
        decrypt(ctxt, ctxt.getDatabasePath(dbName), passphrase);
    }

    public static void decrypt(Context ctxt, String dbName, File decryptedFile, byte[] passphrase) throws IOException {
        decrypt(ctxt, ctxt.getDatabasePath(dbName), decryptedFile, passphrase);
    }

    /**
     * Replaces this database with a decrypted version, deleting the original
     * encrypted database. Do not call this while the database is open, which
     * includes during any Room migrations.
     * <p>
     * The passphrase is untouched in this call. Please set all bytes of the
     * passphrase to 0 or something to clear out the passphrase if you are done
     * with it.
     *
     * @param ctxt         a Context
     * @param originalFile a File pointing to the encrypted database
     * @param passphrase   the passphrase from the user for the encrypted database
     * @throws IOException
     */
    public static void decrypt(Context ctxt, File originalFile, byte[] passphrase)
            throws IOException {

        if (originalFile.exists()) {
            File newFile =
                    File.createTempFile("sqlcipherutils", "tmp",
                        originalFile.getParentFile());
            decrypt(ctxt, originalFile, newFile, passphrase);
            replaceDatabaseFile(newFile, originalFile);
        }

    }

    public static void decrypt(Context ctxt, File originalFile, File decryptedFile, byte[] passphrase) throws IOException {
        System.loadLibrary("sqlcipher");

        if (originalFile.exists()) {
            SQLiteDatabase db =
                    SQLiteDatabase.openDatabase(originalFile.getAbsolutePath(),
                            passphrase, null, SQLiteDatabase.OPEN_READWRITE, null, null);

            final SQLiteStatement st = db.compileStatement("ATTACH DATABASE ? AS plaintext KEY ''");

            if(decryptedFile.exists()){
                decryptedFile.delete();
            }
            decryptedFile.createNewFile();
            st.bindString(1, decryptedFile.getAbsolutePath());
            st.execute();

            db.rawExecSQL("SELECT sqlcipher_export('plaintext')");
            db.rawExecSQL("DETACH DATABASE plaintext");

            int version = db.getVersion();

            st.close();
            db.close();

            db = SQLiteDatabase.openDatabase(decryptedFile.getAbsolutePath(), "", 
                    null, SQLiteDatabase.OPEN_READWRITE, null, null);
            db.setVersion(version);
            db.close();
        } else {
            throw new FileNotFoundException(originalFile.getAbsolutePath() + " not found");
        }
    }

    public static byte[] getBytes(char[] data) {
        if (data != null && data.length != 0) {
            CharBuffer charBuffer = CharBuffer.wrap(data);
            ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
            byte[] result = new byte[byteBuffer.limit()];
            byteBuffer.get(result);
            return result;
        } else {
            return null;
        }
    }
}