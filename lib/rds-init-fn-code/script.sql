/* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved. */
/* SPDX-License-Identifier: MIT-0 */

/* case-sensitive */


DROP SCHEMA IF EXISTS `FileTransferAdminPortal` ;
CREATE SCHEMA IF NOT EXISTS `FileTransferAdminPortal`;

DROP USER IF EXISTS svc_fap;
CREATE USER svc_fap IDENTIFIED WITH AWSAuthenticationPlugin AS 'RDS';
GRANT SELECT, INSERT, UPDATE, DELETE on FileTransferAdminPortal.* TO svc_fap@'%'; 


USE `FileTransferAdminPortal` ;

CREATE TABLE `organization` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(200) NOT NULL,
  `active` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `AppUser` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `firstName` varchar(255) DEFAULT NULL,
  `lastName` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `passwordExpiration` datetime DEFAULT NULL,
  `role` varchar(100) DEFAULT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `organizationId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`username`),
  CONSTRAINT `fk_userToOrganization`
    FOREIGN KEY (`organizationId`) REFERENCES `organization` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=latin1; 
 
CREATE TABLE IF NOT EXISTS `directoryMapping` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `entry` text,
  `target` text,
  `read` bit(1) NOT NULL DEFAULT b'1',
  `write` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_mappingToUser`
    FOREIGN KEY (`userId`) REFERENCES `AppUser` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=latin1;
 
CREATE TABLE IF NOT EXISTS `emailLog` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dateSent` datetime NOT NULL,
  `subject` text,
  `body` longtext,
  `recipients` longtext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=latin1;

CREATE TABLE `keys` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `s3PrivateKeyPath` varchar(255) NOT NULL,
  `s3PublicKeyPath` varchar(255) NOT NULL,
  `created` datetime NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_keysToUser`
    FOREIGN KEY (`userId`) REFERENCES `AppUser` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `TransferSite` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,  
  `transferSiteName` varchar(255) DEFAULT NULL,
  `pathPrefix` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=latin1;

CREATE TABLE `TransferSiteMembership` (
  `username` varchar(255) NOT NULL,
  `transferSiteId` bigint(20) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`username`, `transferSiteId`),
  CONSTRAINT `Constr_TransferSiteMembership_Username_fk`
    FOREIGN KEY `Username_fk` (`username`) REFERENCES `AppUser` (`username`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `Constr_TransferSiteMembership_TransferSiteId_fk`
    FOREIGN KEY `TransferSite_fk` (`transferSiteId`) REFERENCES `TransferSite` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=latin1;

INSERT INTO organization (id, description, active) VALUES (?,?,?);
INSERT INTO AppUser (id, firstName, lastName, username, email, password, passwordExpiration, role, enabled, organizationId) VALUES (?,?,?,?,?,?,?,?,?,?);

     