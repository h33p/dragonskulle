const mongoose = require('mongoose');
const HostSchema = new mongoose.Schema({  
  address: String,
  port: Number
});
module.exports = mongoose.model('Host', HostSchema);