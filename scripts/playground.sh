#!/bin/bash
#TO BE DELETED BEFORE MERGIN PR
commitMessage='[skipTests] message [trigger-release] [devRelease=6.3.0-SNAPSHOT] [release=6.3.0-A5][comRelease=202003] adding some nonsense to test the message -8y-18hrnfjif1][]1[]1r[1][f3][]3g[g]g[[]3[]g[4][]4]g4][3]455[5][6]34[]g[]g[42[r1r31443=1=3=411=3]'

# This echos each string of the commit message that is encased in square brackets
# Example commit message: "Commit message [message 1][message 2]" will echo "message 1" "message 2"
collectVariableTriggers() {
    local regex='\[\K[^\]]*=[^\]]*(?=\])'
    local s=$1 
    echo $s | grep -Po $regex
}

# Get the variable from a string
extractVariable() {
    local regex="=\K.*"
    local s=$1
    echo $s | grep -Po $regex
}

# Get the variable name from the string
extractVariableName() {
    local s=$1 regex='.*(?==)'
    echo $s | grep -Po $regex
}

# Lets create an array containing section of the commit message in the format [*=*] ie [devRelease=repo-4735] will be added as devRelease=repo-4735
mapfile -t commitsVariables < <( echo $commitMessage | grep -Po '\[\K[^\]]*=[^\]]*(?=\])' )
for i in ${commitsVariables[@]}
do
    variable=$(echo $i | grep -Po '=\K.*' )
    variableName=$(echo $i | grep -Po '.*(?==)')
    case  $variableName  in
            "devRelease")      
                devRelease="$variable"
                ;;
            "release")
                release="$variable"
                ;;            
            "comRelease")
                comRelease="$variable"     
                ;;
            *)              
    esac
done

echo "devRelease is equal to $devRelease"
echo "release is equal to $release"
echo "comRelease is equal to $comRelease"

echo "Let's assign some variables directly from the commit message"
releaseVersion=$(echo $commitMessage | grep -Po '\[release[^\]]*=\K[^\]]*(?=\])')
developmentVersion=$(echo $commitMessage | grep -Po '\[devRelease[^\]]*=\K[^\]]*(?=\])')
echo $releaseVersion
echo $developmentVersion

echo "Let's assign some variables directly from the commit message but this time with variable names!"
releaseVersionVariableName="release"
devVersionVariableName="devRelease"
echo $releaseVersionVariableName
echo $devVersionVariableName

regex1="\[${releaseVersionVariableName}[^\]]*=\K[^\]]*(?=\])"

echo $regex1
releaseVersion=$(echo $commitMessage | grep -Po $regex1)
echo $releaseVersion

echo $commitMessage | grep -Po "\[{$devVersionVariableName}[^\]]*=\K[^\]]*(?=\])"

. ./travis/common_function.sh
extractVariable "release" "$commitMessage"