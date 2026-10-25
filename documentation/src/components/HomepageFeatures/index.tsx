import type {ReactNode} from 'react';
import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';

type FeatureItem = {
  title: string;
  image: React.ComponentType<React.ComponentProps<'svg'>>;
};

const FeatureList: FeatureItem[] = [
  {
    title: 'Easy to Use',
    image: require('@site/static/img/phoneScreenshots/01_ManageAndOrganize.png').default,
  },
  {
    title: 'Focus on What Matters',
    image: require('@site/static/img/phoneScreenshots/03_TaskLists.png').default,
  },
  {
    title: 'Rich Formatting',
    image: require('@site/static/img/phoneScreenshots/04_RichText.png').default,
  },
  {
    title: 'Labels & Organization',
    image: require('@site/static/img/phoneScreenshots/05_ImagesAndFiles.png').default,
  },
  {
    title: 'Search & Filters',
    image: require('@site/static/img/phoneScreenshots/06_Widgets.png').default,
  },
  {
    title: 'Dark Mode & Custom Themes',
    image: require('@site/static/img/phoneScreenshots/07_Reminders.png').default,
  },
  {
    title: 'Security & Backup',
    image: require('@site/static/img/phoneScreenshots/08_DarkMode.png').default,
  },
  {
    title: 'Extensive Settings',
    image: require('@site/static/img/phoneScreenshots/09_FingerprintLock.png').default,
  },
];

function Feature({title, image, description}: FeatureItem) {
  return (
    <div className={clsx('col col--3')}>
      <div className="text--center">
        <img src={image} alt={title} />
      </div>
    </div>
  );
}

export default function HomepageFeatures(): ReactNode {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
