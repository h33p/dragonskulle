import Config from './models/Config';

const Host = require('./models/Config');
const connectToDatabase = require('./db');
'use strict';

export default function handler(req, res) {
    var contype = req.headers['content-type'];
    var api_key = req.headers['token'];
    if (req.method === 'POST') {
        if (api_key != process.env.TOKEN) {
            res.status(400).json({ 'body': 'Invalid API Key.' });
            return;
        }
        if (!contype || contype.indexOf('application/json') !== 0) {
            return new Promise((resolve, _reject) => {
                res.status(400).json({ 'body': 'Body must be JSON.' });
                resolve();
            });
        } else {
            return new Promise((resolve, _reject) => {
                connectToDatabase()
                    .then(() => {
                        Config.findByIdAndUpdate("609d206a7e3e1c72ac97737e", {
                            global: req.body.global,
                            attackStat: req.body.attackStat,
                            buildDistanceStat: req.body.buildDistanceStat,
                            claimDistanceStat: req.body.claimDistanceStat,
                            defenceStat: req.body.defenceStat,
                            generationStat: req.body.generationStat,
                            viewDistanceStat: req.body.viewDistanceStat,
                            player: req.body.player,
                            ai: req.body.ai
                        }, { upsert: true }, (err, entry) => {
                            if (err) {
                                console.error(err)
                                res.setHeader('content-type', 'text/plain');
                                res.status(err.statusCode || 500).json({ 'body': 'Could not update config.', "success": false });
                                resolve();
                            }
                            res.status(200).json({ "success": true });
                            resolve();
                        });
                    })
            });
        }
    } else if (req.method === 'GET') {
        return new Promise((resolve, _reject) => {
            connectToDatabase()
                .then(() => {
                    Host.findOne()
                        .then(hosts => {
                            res.status(200).json(hosts);
                            resolve();
                        })
                        .catch(err => {
                            res.setHeader('content-type', 'text/plain');
                            res.status(err.statusCode || 500).json({ 'body': 'Could not fetch the hosts.' });
                            resolve();
                        });
                });
        });
    }
}
