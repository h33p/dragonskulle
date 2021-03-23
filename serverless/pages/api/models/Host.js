const mongoose = require('mongoose');
const HostSchema = new mongoose.Schema({
  address: String,
  port: Number
},
  { timestamps: true }
);

module.exports = mongoose.models.Host || mongoose.model('Host', HostSchema);