#!/bin/bash -ex
# Script: cherry-pick-old-projects-setup.sh
#
# ***********************************************************************************************************
# ***** BEFORE RUNNING, edit the calls to setupRemotes (approx line 100) to specify the source branch  ******
# ***** (2nd arg) to be checked out.                                                                   ******
# ***********************************************************************************************************
#
# Clones and moves files in local copies of the the original repository projects to make it easier to cherry
# pick commits into alfresco-community-repo and alfresco-enterprise-repo. Run from a folder that contains
# alfresco-enterprise-repo and alfresco-enterprise-repo projects. The clones of the original projects are
# placed in a folder called oldRepos. Once run these projects will be set up as remotes to the new projects
# ready to allow cherry picking of commits.

if [[ ! -d alfresco-community-repo || ! -d alfresco-enterprise-repo ]]
then
  echo "Script MUST be run in the parent folder of alfresco-community-repo and alfresco-enterprise-repo"
  exit 1
fi

baseDir=`pwd`
oldReposDirName=oldRepos
oldReposDir=$baseDir/$oldReposDirName
mkdir -p $oldReposDir

#
# Moves the contents of the current folder to a new sub folder.
#
moveContent()
{
	oldFolder=$1
	newModule=$2

  mkdir -p ${newModule}/$oldFolder
	for file in `ls -a $oldFolder`
    do
        if [[ $file != ".git" && $file != ".." && $file != "." && $file != ".tmp" && $file != "${newModule}" ]]
        then
            git mv $oldFolder/$file ${newModule}/$oldFolder
        fi
    done

    git commit -a -m "Moved content to new ${newModule} module" | grep -v "rename "
}

mvCommit()
{
	from=$1
	to=$2

  echo git mv $from $to
  git      mv $from $to

  if [[ -z "$(ls -A .tmp)" ]]
  then
    rmdir .tmp
  fi

  git commit -a -m "Moved $from to $to" | grep -v "rename "
}

#
# Moves all the root folder contents of a branch to a named folder in the root folder.
#
setupRemotes()
{
  oldRepo=$1
  oldBranch=$2
  newRepo=$3
  newModule=$4

  echo
  echo
  cd  $oldReposDir
  rm -rf $oldRepo
  git clone --single-branch --branch $oldBranch git@github.com:Alfresco/${oldRepo}.git
  cd  $oldRepo

  count=`find . -type f | grep -v '^\.\/.git\/' | wc -l`
	echo Move $count files in $oldRepo

	# As the alfresco-repository src folder has so many files, the git mv and git commit commands fail,
	# so let's do it in several parts
  if [[ "$oldRepo" = "alfresco-repository" ]]
  then
    mkdir .tmp
    mvCommit src/main/java/org/alfresco/repo .tmp/repo
    mvCommit src/main/resources              .tmp/resources
    moveContent . $newModule
    mvCommit .tmp/repo      ${newModule}/src/main/java/org/alfresco/repo
    mvCommit .tmp/resources ${newModule}/src/main/resources
  else
    moveContent . $newModule
  fi
}

#            source project                 branch   target project          module
setupRemotes alfresco-core                  master   alfresco-community-repo core
setupRemotes alfresco-data-model            master   alfresco-community-repo data-model
setupRemotes alfresco-repository            master   alfresco-community-repo repository
setupRemotes alfresco-remote-api            master   alfresco-community-repo remote-api
setupRemotes acs-community-packaging        develop  alfresco-community-repo packaging

setupRemotes alfresco-enterprise-repository master   alfresco-enterprise-repo repository
setupRemotes alfresco-enterprise-remote-api master   alfresco-enterprise-repo remote-api
setupRemotes acs-packaging                  master   alfresco-enterprise-repo packaging



#
# Add remotes and fetch original projects
#

echo
echo
cd $baseDir

cd alfresco-community-repo
git remote add alfresco-core                  ../$oldReposDirName/alfresco-core
git remote add alfresco-data-model            ../$oldReposDirName/alfresco-data-model
git remote add alfresco-repository            ../$oldReposDirName/alfresco-repository
git remote add alfresco-remote-api            ../$oldReposDirName/alfresco-remote-api
git remote add acs-community-packaging        ../$oldReposDirName/acs-community-packaging
git config merge.renameLimit 999999
git fetch alfresco-core                       | grep -v 'new tag]'
git fetch alfresco-data-model                 | grep -v 'new tag]'
git fetch alfresco-repository                 | grep -v 'new tag]'
git fetch alfresco-remote-api                 | grep -v 'new tag]'
git fetch acs-community-packaging             | grep -v 'new tag]'
cd ..

cd alfresco-enterprise-repo
git remote add alfresco-enterprise-repository ../$oldReposDirName/alfresco-enterprise-repository
git remote add alfresco-enterprise-remote-api ../$oldReposDirName/alfresco-enterprise-remote-api
git remote add acs-packaging                  ../$oldReposDirName/acs-packaging
git config merge.renameLimit 999999
git fetch alfresco-enterprise-repository      | grep -v 'new tag]'
git fetch alfresco-enterprise-remote-api      | grep -v 'new tag]'
git fetch acs-packaging                       | grep -v 'new tag]'
cd ..






#
# remote remove repositories
#
echo You may now cherry pick to alfresco-community-repo and alfresco-enterprise-repo
echo after you press RETURN the remotes will be removed.
read DONE

cd alfresco-community-repo
git remote remove alfresco-core
git remote remove alfresco-data-model
git remote remove alfresco-repository
git remote remove alfresco-remote-api
git remote remove acs-community-packaging
git config --unset merge.renameLimit
cd ..

cd alfresco-enterprise-repo
git remote remove alfresco-enterprise-repository
git remote remove alfresco-enterprise-remote-api
git remote remove acs-packaging
git config --unset merge.renameLimit
cd ..

rm -rf $oldReposDir