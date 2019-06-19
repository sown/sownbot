# SOWN-Bot

SOWN-Bot is a bot that sits in SOWN's IRC channel (#sown on irc.sown.org.uk).  It uses the PircBot Java IRC Bot Framework.  As of June 2019 the framework JAR files it is using are rather out of date and could do with some updating.

SOWN-Bot has a number of modules it uses to interact with people in #sown:
* Ops management
* Icinga monitoring
* Events management (inc. setting topic)
* Subversion and Git reporting
* Logging (inc. censoring messages from public logs)

We are looking to add further modules in the future:
* GitHub interaction

## Existing Modules

### Ops Management
SOWN-Bot has access to a database to determine which users should be given operator privileges.  These should be granted when the user joins the chanell or when they run the ***!op*** command.  The ***!op all*** command should give operator privileges to all registered operators who do not yet have these privileges.  SOWN-Bot requires operator privileges itself to be able to do this.

### Icinga Monitoring
***To be written***

### Events Management
***To be written***

### Subversion and Git Reporting
***To be written***

### Logging
***To be written***

## New Modules

### GitHub Interaction
As SOWN tries to make more extensive use of GitHub we would like SOWN-Bot to interact with it.  In particular we have replaced our old bespoke todo system to use GitHub issues instead.  SOWN-Bot use to have a module to interact with ths bespoke todo system.  It would be nice if we could write something similar to work with GitHub issues.  

Beyond that something that would allow us to interact with otehr aspects of GitHub would be useful.  Currently the node\_control repository has a post push hook that cause a GitHub bot to enter the channel and detail the latest pushed changesets.
