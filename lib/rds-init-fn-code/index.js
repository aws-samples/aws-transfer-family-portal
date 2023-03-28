const mysql = require('mysql')
const AWS = require('aws-sdk')
const fs = require('fs')
const path = require('path')
/*const bcrypt = require("bcrypt")*/
const secrets = new AWS.SecretsManager({apiVersion: '2017-10-17'})
const secretName = "FileTransferPortalInitialCreds"
exports.handler = async (e) => {
  try {
    const { config } = e.params
    
    /*Grab connection info for the db from secrets manager*/
    const {password, username, host } = await getSecretValue(config.credsSecretName)
    const initialPasswordHash = config.pashword;
    const today = new Date();
    const passwordExpiration = new Date(today.getFullYear() + 1, today.getMonth(), today.getDate());
    const queryParameters = [
      1, //id
      "Organization", //description,
      1, //active
      
      1, //id
      "Admin", //firstName,
      "Administrator", //lastName,
      "admin", //username,
      "change_me@youremail.com", //email. 
      initialPasswordHash, //hashed password,
      passwordExpiration, //passwordExpiration,
      "ROLE_ADMIN", //role,
      1, //enabled,
      1, //organizationId,
    ];
    /**/
   
   /*Read the SQL statements from the file into a variable*/
   const dirtySql = fs.readFileSync(path.join(__dirname, 'script.sql')).toString()
   
   /*Substitute the parameter values*/
   const cleanSql = mysql.format(dirtySql, queryParameters);
   const connection = mysql.createConnection({
      host: host,
      user: username,
      password: password,
      multipleStatements: true
    })

    connection.connect()
    
    const res = await query(connection, cleanSql)
    connection.end()
    
    return {
      status: 'OK'/*,
      results: res*/
    }
  } catch (err) {
    return {
      status: 'ERROR',
      err,
      message: err.message
    }
  }
}

function query (connection, sql) {
  return new Promise((resolve, reject) => {
    connection.query(sql, (error, res) => {
      if (error) return reject(error)

      return resolve(res)
    })
  })
}

function getSecretValue (secretId) {
  return new Promise((resolve, reject) => {
    secrets.getSecretValue({ SecretId: secretId }, (err, data) => {
      if (err) return reject(err)

      return resolve(JSON.parse(data.SecretString))
    })
  })
}
