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
    image: require('@site/static/img/phoneScreenshots/1.png').default,
  },
  {
    title: 'Focus on What Matters',
    image: require('@site/static/img/phoneScreenshots/2.png').default,
  },
  {
    title: 'Rich Formatting',
    image: require('@site/static/img/phoneScreenshots/3.png').default,
  },
  {
    title: 'Labels & Organization',
    image: require('@site/static/img/phoneScreenshots/4.png').default,
  },
  {
    title: 'Search & Filters',
    image: require('@site/static/img/phoneScreenshots/5.png').default,
  },
  {
    title: 'Dark Mode & Custom Themes',
    image: require('@site/static/img/phoneScreenshots/6.png').default,
  },
  {
    title: 'Security & Backup',
    image: require('@site/static/img/phoneScreenshots/7.png').default,
  },
  {
    title: 'Extensive Settings',
    image: require('@site/static/img/phoneScreenshots/8.png').default,
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
