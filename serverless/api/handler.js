require('dotenv').config({ path: './.env' });
const Host = require('./models/Host');
const connectToDatabase = require('./db');
'use strict';

module.exports.create = (event, context, callback) => {
  context.callbackWaitsForEmptyEventLoop = false;

  connectToDatabase()
    .then(() => {
      Host.create(JSON.parse(event.body))
        .then(host => callback(null, {
          statusCode: 200,
          body: JSON.stringify(host)
        }))
        .catch(err => callback(null, {
          statusCode: err.statusCode || 500,
          headers: { 'Content-Type': 'text/plain' },
          body: 'Could not create the host.'
        }));
    });
};

module.exports.getOne = (event, context, callback) => {
  context.callbackWaitsForEmptyEventLoop = false;

  connectToDatabase()
    .then(() => {
        Host.findById(event.pathParameters.id)
        .then(host => callback(null, {
          statusCode: 200,
          body: JSON.stringify(host)
        }))
        .catch(err => callback(null, {
          statusCode: err.statusCode || 500,
          headers: { 'Content-Type': 'text/plain' },
          body: 'Could not fetch the host.'
        }));
    });
};

module.exports.getAll = (event, context, callback) => {
  context.callbackWaitsForEmptyEventLoop = false;

  connectToDatabase()
    .then(() => {
      Host.find()
        .then(hosts => callback(null, {
          statusCode: 200,
          body: JSON.stringify(hosts)
        }))
        .catch(err => callback(null, {
          statusCode: err.statusCode || 500,
          headers: { 'Content-Type': 'text/plain' },
          body: 'Could not fetch the hosts.'
        }))
    });
};


module.exports.delete = (event, context, callback) => {
  context.callbackWaitsForEmptyEventLoop = false;

  connectToDatabase()
    .then(() => {
      Host.findByIdAndRemove(event.pathParameters.id)
        .then(host => callback(null, {
          statusCode: 200,
          body: JSON.stringify({ message: 'Removed host with id: ' + host._id, host: host })
        }))
        .catch(err => callback(null, {
          statusCode: err.statusCode || 500,
          headers: { 'Content-Type': 'text/plain' },
          body: 'Could not fetch the host.'
        }));
    });
};