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
          <pre><code>{
            <span class="hljs-attr">"global"</span> : {
              <span class="hljs-attr">"inflation"</span> : <span class="hljs-number">1.002</span>,
    <span class="hljs-attr">"mapSize"</span> : <span class="hljs-number">51</span>
  },
  <span class="hljs-attr">"player"</span> : {
              <span class="hljs-attr">"attackCooldown"</span> : <span class="hljs-number">2.0</span>,
    <span class="hljs-attr">"tokenRate"</span> : <span class="hljs-number">5.0</span>,
    <span class="hljs-attr">"tokenTime"</span> : <span class="hljs-number">1.0</span>,
    <span class="hljs-attr">"inflationPerBuilding"</span> : <span class="hljs-number">1.05</span>,
    <span class="hljs-attr">"attackHeightMul"</span> : <span class="hljs-number">1.0</span>
  },
  <span class="hljs-attr">"ai"</span> : {
              <span class="hljs-attr">"lowerBoundTime"</span> : <span class="hljs-number">1.0</span>,
    <span class="hljs-attr">"upperBoundTime"</span> : <span class="hljs-number">2.0</span>
  },
  <span class="hljs-attr">"probabilisticAi"</span> : {
              <span class="hljs-attr">"buildProbability"</span> : <span class="hljs-number">0.65</span>,
    <span class="hljs-attr">"upgradeProbability"</span> : <span class="hljs-number">0.15</span>,
    <span class="hljs-attr">"sellProbability"</span> : <span class="hljs-number">0.05</span>,
    <span class="hljs-attr">"attackProbability"</span> : <span class="hljs-number">0.15</span>
  },
  <span class="hljs-attr">"aiAimer"</span> : {
              <span class="hljs-attr">"playAStar"</span> : <span class="hljs-number">0.9</span>,
    <span class="hljs-attr">"aimAtCapital"</span> : <span class="hljs-number">0.01</span>,
    <span class="hljs-attr">"tries"</span> : <span class="hljs-number">10</span>
  },
  <span class="hljs-attr">"attackStat"</span> : {
              <span class="hljs-attr">"value"</span> : {
              <span class="hljs-attr">"bonus"</span> : {
              <span class="hljs-attr">"multiplier"</span> : <span class="hljs-number">0.0</span>,
        <span class="hljs-attr">"bonusTile"</span> : <span class="hljs-literal">null</span>
      },
      <span class="hljs-attr">"baseValue"</span> : <span class="hljs-number">0.0</span>,
      <span class="hljs-attr">"mulLevel"</span> : <span class="hljs-number">1.0</span>,
      <span class="hljs-attr">"minValue"</span> : <span class="hljs-number">0.0</span>,
      <span class="hljs-attr">"maxValue"</span> : <span class="hljs-number">100.0</span>
    },
    <span class="hljs-attr">"cost"</span> : {
              <span class="hljs-attr">"selfLevelMultiplier"</span> : <span class="hljs-number">3.0</span>,
    }
  },
  <span class="hljs-attr">"buildDistanceStat"</span> : {
              <span class="hljs-attr">"value"</span> : {
              <span class="hljs-attr">"bonus"</span> : {
              <span class="hljs-attr">"multiplier"</span> : <span class="hljs-number">0.0</span>,
        <span class="hljs-attr">"bonusTile"</span> : <span class="hljs-literal">null</span>
      },
      <span class="hljs-attr">"baseValue"</span> : <span class="hljs-number">2.0</span>,
      <span class="hljs-attr">"mulLevel"</span> : <span class="hljs-number">0.0</span>,
      <span class="hljs-attr">"minValue"</span> : <span class="hljs-number">0.0</span>,
      <span class="hljs-attr">"maxValue"</span> : <span class="hljs-number">100.0</span>
    },
    <span class="hljs-attr">"cost"</span> : {
              <span class="hljs-attr">"selfLevelMultiplier"</span> : <span class="hljs-number">3.0</span>,
    }
  },
  <span class="hljs-attr">"claimDistanceStat"</span> : {
              <span class="hljs-attr">"value"</span> : {
              <span class="hljs-attr">"bonus"</span> : {
              <span class="hljs-attr">"multiplier"</span> : <span class="hljs-number">0.0</span>,
        <span class="hljs-attr">"bonusTile"</span> : <span class="hljs-literal">null</span>
      },
      <span class="hljs-attr">"baseValue"</span> : <span class="hljs-number">1.0</span>,
      <span class="hljs-attr">"mulLevel"</span> : <span class="hljs-number">0.0</span>,
      <span class="hljs-attr">"minValue"</span> : <span class="hljs-number">0.0</span>,
      <span class="hljs-attr">"maxValue"</span> : <span class="hljs-number">100.0</span>
    },
    <span class="hljs-attr">"cost"</span> : {
              <span class="hljs-attr">"selfLevelMultiplier"</span> : <span class="hljs-number">3.0</span>,
    }
  },
  <span class="hljs-attr">"defenceStat"</span> : {
              <span class="hljs-attr">"value"</span> : {
              <span class="hljs-attr">"bonus"</span> : {
              <span class="hljs-attr">"multiplier"</span> : <span class="hljs-number">0.5</span>,
        <span class="hljs-attr">"bonusTile"</span> : <span class="hljs-string">"MOUNTAIN"</span>
      },
      <span class="hljs-attr">"baseValue"</span> : <span class="hljs-number">-1.0</span>,
      <span class="hljs-attr">"mulLevel"</span> : <span class="hljs-number">1.0</span>,
      <span class="hljs-attr">"minValue"</span> : <span class="hljs-number">0.0</span>,
      <span class="hljs-attr">"maxValue"</span> : <span class="hljs-number">100.0</span>
    },
    <span class="hljs-attr">"cost"</span> : {
              <span class="hljs-attr">"selfLevelMultiplier"</span> : <span class="hljs-number">3.0</span>,
    }
  },
  <span class="hljs-attr">"generationStat"</span> : {
              <span class="hljs-attr">"value"</span> : {
              <span class="hljs-attr">"bonus"</span> : {
              <span class="hljs-attr">"multiplier"</span> : <span class="hljs-number">1.0</span>,
        <span class="hljs-attr">"bonusTile"</span> : <span class="hljs-string">"WATER"</span>
      },
      <span class="hljs-attr">"baseValue"</span> : <span class="hljs-number">-1.0</span>,
      <span class="hljs-attr">"mulLevel"</span> : <span class="hljs-number">1.0</span>,
      <span class="hljs-attr">"minValue"</span> : <span class="hljs-number">0.0</span>,
      <span class="hljs-attr">"maxValue"</span> : <span class="hljs-number">100.0</span>
    },
    <span class="hljs-attr">"cost"</span> : {
              <span class="hljs-attr">"selfLevelMultiplier"</span> : <span class="hljs-number">3.0</span>,
    }
  },
  <span class="hljs-attr">"viewDistanceStat"</span> : {
              <span class="hljs-attr">"value"</span> : {
              <span class="hljs-attr">"bonus"</span> : {
              <span class="hljs-attr">"multiplier"</span> : <span class="hljs-number">0.0</span>,
        <span class="hljs-attr">"bonusTile"</span> : <span class="hljs-literal">null</span>
      },
      <span class="hljs-attr">"baseValue"</span> : <span class="hljs-number">3.0</span>,
      <span class="hljs-attr">"mulLevel"</span> : <span class="hljs-number">0.0</span>,
      <span class="hljs-attr">"minValue"</span> : <span class="hljs-number">0.0</span>,
      <span class="hljs-attr">"maxValue"</span> : <span class="hljs-number">3.0</span>
    },
    <span class="hljs-attr">"cost"</span> : {
              <span class="hljs-attr">"selfLevelMultiplier"</span> : <span class="hljs-number">3.0</span>,
    }
  }
}
</code></pre>
        </div>
      </div>
    </>
  )
}