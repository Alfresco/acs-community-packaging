#!/usr/bin/env bash

echo "========================== Starting Prepare Staging Deploy Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

# Identify latest annotated tag (latest version)
export VERSION=$(git describe --abbrev=0 --tags)

# Get third party license scripts.
git clone --depth=1 https://github.com/Alfresco/third-party-license-overrides.git

# Move the final artifacts to a single folder (deploy_dir) to be copied to S3
mkdir -p deploy_dir
cp distribution/target/alfresco.war deploy_dir
cp distribution/target/*-distribution*.zip deploy_dir
# Create third party license csv file and add it to the deploy directory.
unzip deploy_dir/*-distribution*.zip -d deploy_dir/community-acs
zippaths=""
for file in `find deploy_dir/community-acs -name "*.amp" -o -name "*.war" -not -name "ROOT.war" -not -name "_vti_bin.war"`
do
    zippaths+="$file|"
done
zippaths=${zippaths::-1}
python3 ./third-party-license-overrides/thirdPartyLicenseCSVCreator.py --zippaths "${zippaths}" --version "${VERSION}" --combined --output "deploy_dir"
rm -rf deploy_dir/community-acs
echo "Local deploy directory content:"
ls -lA deploy_dir

# Create deploy directory for AGS.
mkdir -p deploy_dir_ags
cp distribution-ags/target/*.zip deploy_dir_ags
# Generate third party license csv for AGS.
unzip deploy_dir_ags/*.zip -d deploy_dir_ags/community-ags
zippaths=""
for file in `find deploy_dir_ags/community-ags -name "*.amp" -o -name "*.war"`
do
    zippaths+="$file|"
done
zippaths=${zippaths::-1}
python3 ./third-party-license-overrides/thirdPartyLicenseCSVCreator.py --zippaths ${zippaths} --version "${VERSION}" --combined --output "deploy_dir_ags"
rm -rf deploy_dir_ags/community-ags
echo "Local AGS deploy directory content:"
ls -lA deploy_dir_ags

# Tidy up.
rm -rf ./third-party-license-overrides

popd
set +vex
echo "========================== Finishing Prepare Staging Deploy Script =========================="
