#!/usr/bin/env bash
set -env

build_number=$1
branch_name=$2
release_stage=release
s3_location=s3://alfresco-artefacts-staging/alfresco-content-services-community/$release_stage/$branch_name/$build_number/
echo_s3_location=https://s3.console.aws.amazon.com/s3/buckets/alfresco-artefacts-staging/alfresco-content-services-community/$release_stage

aws s3 cp --acl private war/target/alfresco.war $s3_location
aws s3 cp --acl private distribution/target/*-distribution*.zip $s3_location

mkdir -p target
touch target/README.txt

distribution_zip_name=`ls distribution/target/*-distribution*.zip | xargs -n 1 basename`
