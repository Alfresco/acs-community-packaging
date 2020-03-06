# Will echo value of the provided variablePrefix from the commit message Example useage and output:
# $ extractVariable "release" "[skipTests][release=6.3.0]" 
# $ 6.3.0
extractVariable()
{
    local variablePrefix=$1 commitMessage=$2
    regex="\[$variablePrefix[^\]]*=\K[^\]]*(?=\])" 
    echo $commitMessage | grep -Po $regex
}

echo "common_function.sh loaded"