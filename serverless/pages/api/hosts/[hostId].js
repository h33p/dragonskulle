const Host = require('../models/Host');
const connectToDatabase = require('../db');
'use strict';

export default function handler(req, res) {
    const { hostId } = req.query;
    if (req.method === 'DELETE') {
        return new Promise((resolve, reject) => {
            connectToDatabase()
                .then(() => {
                    Host.findByIdAndRemove(hostId)
                        .then(host => {
                            res.status(200).json({ message: 'Removed host with id: ' + host._id, host: host });
                            resolve();
                        })
                        .catch(err => {
                            res.setHeader('content-type', 'text/plain');
                            res.status(err.statusCode || 500).json({ 'body': 'Could not fetch the host' })
                            resolve();
                        });
                });
        });
    } else if (req.method === 'GET') {
        return new Promise((resolve, reject) => {
            connectToDatabase()
                .then(() => {
                    Host.findById(hostId)
                        .then(host => {
                            res.status(200).json(host);
                            resolve();
                        })
                        .catch(err => {
                            res.setHeader('content-type', 'text/plain');
                            res.status(err.statusCode || 500).json({ 'body': 'Could not fetch the host' })
                            resolve();
                        });
                })
        });
    }
}