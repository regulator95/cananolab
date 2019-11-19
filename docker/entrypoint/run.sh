#!/bin/sh
if [ -d "/usr/local/cananolab/software" ]; then
        echo "Skipping cananolab install"
else
  cp -R /tmp/cananolab /usr/local  
  echo "Moving Repo to /usr/local"
fi

# pull up to date code #
echo "entering cananolab directory"
cd cananolab
echo "pulling latest code from github"
git checkout synthesis
git pull 

JBOSS_HOME=/opt/wildfly-8.2.1.Final
JBOSS_CLI=$JBOSS_HOME/bin/jboss-cli.sh
JBOSS_MODE=${1:-"standalone"}
JBOSS_CONFIG=${2:-"standalone-full.xml"}

function wait_for_server() {
  until `$JBOSS_CLI -c --controller=localhost:19990 ":read-attribute(name=server-state)" 2> /dev/null | grep -q running`; do
    sleep 1
  done
}

#start server #
echo "starting wildfly"
$JBOSS_HOME/bin/$JBOSS_MODE.sh -b 0.0.0.0 -c $JBOSS_CONFIG &

echo "=> Waiting for the server to boot"
wait_for_server

echo "=> deploying "
/opt/wildfly-8.2.1.Final/bin/run.sh

echo "=> Shutting down WildFly"
if [ "$JBOSS_MODE" = "standalone" ]; then
  $JBOSS_CLI -c --controller=localhost:19990 ":shutdown"
fi


echo "=> Restarting WildFly"
$JBOSS_HOME/bin/$JBOSS_MODE.sh -b 0.0.0.0 -c $JBOSS_CONFIG

#$JBOSS_HOME/bin/standalone.sh -b 0.0.0.0 --server-config=standalone-full.xml

