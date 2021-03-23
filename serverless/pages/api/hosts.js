const Host = require('./models/Host');
const connectToDatabase = require('./db');
'use strict';

export default function handler(req, res) {
    var contype = req.headers['content-type'];
    if (req.method === 'POST') {
        if (!contype || contype.indexOf('application/json') !== 0) {
            return new Promise((resolve, reject) => {
                res.status(400).json({ 'body': 'Body must be JSON.' });
                resolve();
            });
        } else {
            return new Promise((resolve, reject) => {
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
        return new Promise((resolve, reject) => {
            connectToDatabase()
                .then(() => {
                    Host.find()
                        .then(hosts => { res.status(200).json(hosts); resolve(); })
                        .catch(err => {
                            res.setHeader('content-type', 'text/plain');
                            res.status(err.statusCode || 500).json({ 'body': 'Could not fetch the hosts.' });
                            resolve();
                        });
                });
        });
    }
}