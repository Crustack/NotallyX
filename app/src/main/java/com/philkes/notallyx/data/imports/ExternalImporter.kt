package com.philkes.notallyx.data.imports

import android.app.Application
import android.net.Uri
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.presentation.view.misc.Progress
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow

interface ExternalImporter {

    /**
     * Parses [BaseNote]s from [source] and copies attached files/images/audios to [destination]
     *
     * @return List of [BaseNote]s to import + folder containing attached files (if no attached
     *   files possible, return null).
     */
    fun import(
        app: Application,
        source: Uri,
        destination: File,
        progress: MutableStateFlow<Progress?>? = null,
    ): Pair<List<BaseNote>, File?>
}
