import Head from 'next/head'
import Layout from '../components/layout'
import React from "react";

export default function Home() {
  return (
    <Layout home>
      <Head>
        <title>DragonSkulle</title>
      </Head>
      <div>
        <div style={{ width: '100%', height: 0, paddingBottom: '100%', position: 'relative' }}><iframe src="https://giphy.com/embed/U1eYjTgjUrlN6" width="100%" height="100%" style={{ position: 'absolute' }} frameBorder={0} className="giphy-embed" allowFullScreen /></div><p><a href="https://giphy.com/gifs/dragon-httyd2-U1eYjTgjUrlN6">via GIPHY</a></p>
      </div>
    </Layout >
  )
}