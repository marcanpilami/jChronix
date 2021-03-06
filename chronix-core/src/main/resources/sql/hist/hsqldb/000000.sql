CREATE CACHED TABLE RunLog(id VARCHAR(36) NOT NULL PRIMARY KEY, visible BOOLEAN, applicationId VARCHAR(36), 
chainId VARCHAR(36), stateId VARCHAR(36), activeNodeId VARCHAR(36), chainLev1Id VARCHAR(36), 
executionNodeId VARCHAR(36), placeId VARCHAR(36), chainLaunchId VARCHAR(36), chainName VARCHAR(100), 
applicationName VARCHAR(100), chainLev1Name VARCHAR(100), activeNodeName VARCHAR(100), placeName VARCHAR(100), 
executionNodeName VARCHAR(100), dns VARCHAR(100), osAccount VARCHAR(100), whatWasRun VARCHAR(1024), 
resultCode INT, lastKnownStatus VARCHAR(20), shortLog VARCHAR(10000), logPath VARCHAR(124), dataIn INT, 
dataOut INT, calendarName VARCHAR(100), calendarOccurrence VARCHAR(100), sequence INT, enteredPipeAt TIMESTAMP WITH TIME ZONE, 
markedForUnAt TIMESTAMP WITH TIME ZONE, beganRunningAt TIMESTAMP WITH TIME ZONE, stoppedRunningAt TIMESTAMP WITH TIME ZONE, lastLocallyModified TIMESTAMP WITH TIME ZONE);

CREATE MEMORY TABLE PUBLIC.VERSION(ID INT NOT NULL PRIMARY KEY, APPLIED TIMESTAMP  WITH TIME ZONE NOT NULL);
