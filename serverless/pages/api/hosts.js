const Host = require('./models/Host');
import {md5} from "./md5";
const connectToDatabase = require('./db');
'use strict';


export default function handler(req, res) {
    var contype = req.headers['content-type'];
    if (req.method === 'POST') {
        if (!contype || contype.indexOf('application/json') !== 0) {
            return new Promise((resolve, _reject) => {
                res.status(400).json({ 'body': 'Body must be JSON.' });
                resolve();
            });
        } else {
            let base = req.body;
            if(!base.hasOwnProperty("port") || !base.hasOwnProperty("address")){
                return new Promise((resolve, _reject) => {
                    res.status(400).json({ 'body': 'Body must be contain port and address fields.' });
                    resolve();
                })
            }
            let hash = md5(Date.now().toString());
            base["_code"] = hash.substring(0, Math.min(6,hash.length)).toUpperCase();
            
            return new Promise((resolve, _reject) => {
                connectToDatabase()
                    .then(() => {
                        Host.create(req.body)
                            .then(host => {
                                console.log("host: " + host);
                                res.status(200).json(host);
                                resolve();
                            })
                            .catch(err => {
                                res.setHeader('content-type', 'text/plain');
                                res.status(err.statusCode || 500).json({ 'body': 'Could not create the host.' });
                                resolve();
                            });
                    })
            });
        }
    } else if (req.method === 'GET') {
        return new Promise((resolve, _reject) => {
            connectToDatabase()
                .then(() => {
                    Host.find()
                        .then(hosts => {
                            var removed = [];
                            hosts.forEach(host => {
                                let dt = Date.parse(host.updatedAt);
                                if (dt + 432000 < Date.now()) {
                                    Host.findByIdAndRemove(host._id)
                                        .catch(() => {
                                        });
                                    removed.push(host._id);
                                }
                            })
                            res.status(200).json(hosts.filter(s => !removed.includes(s._id)));
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
