import React from "react";

export default function Home() {
  return (
    <>
      <div>
        <div style={{ maxWidth: '100%', maxHeight: "100%" }}>
          <iframe src="https://giphy.com/embed/U1eYjTgjUrlN6" frameBorder={0} className="giphy-embed" allowFullScreen />
        </div>
        <div>
          <h1>Serverless API
          This is for keeping track of available non LAN hosts</h1>
          <h2>Endpoints</h2>
          <p>GET /api/hosts - gets all hosts</p>
          <p>PUSH /api/hosts - creates a new entry, must be in format </p>
          <pre><code className="lang-json">{"{"}{"\n"}{"    "}<span className="hljs-attr">"address"</span>: <span className="hljs-string">"255.255.255.255"</span>,{"\n"}{"    "}<span className="hljs-attr">"port"</span>: <span className="hljs-number">1337</span>{"\n"}{"}"}{"\n"}</code></pre>
          <p>DELETE /api/hosts/{'{'}id{'}'} - deletes entry</p>
          <p>GET /api/hosts/{'{'}id{'}'} - retrieves a specific entry</p>
          <p>GET /api/code/{'{'}code{'}'} - gets host by code</p>
          <p>DELETE /api/code/{'{'}code{'}'} - deletes host by code</p>
          <p>GET /api/config - gets the current config values</p>
          <p>POST /api/config - sets the values
          You must also provide <code>token</code> in the headers with the API Key.</p>
    </div>
      </div>
    </>
  )
}
