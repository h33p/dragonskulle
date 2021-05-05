const Host = require('../../models/Host');
const connectToDatabase = require('../../db');
'use strict';

export default function handler(req, res) {
    const { codeId } = req.query;
    var matcher = {
        _code: codeId,
    };
    if (req.method === 'DELETE') {
        return new Promise((resolve, reject) => {
            connectToDatabase()
                .then(() => {
                    Host.deleteOne(matcher, function (err, someValue) {
                        if (err) {
                            res.status(400).json([]);
                        }else{
                            res.send(someValue);
                        }
                        resolve()
                    });
                });
        });
    } else if (req.method === 'GET') {
        return new Promise((resolve, reject) => {
            connectToDatabase()
                .then(() => {
                    Host.findOne(matcher, function (err, someValue) {
                        if (err) {
                            res.status(400).json([]);
                        }else{
                            res.send(someValue);
                        }
                        resolve()
                    });

                })
        });
    }
}