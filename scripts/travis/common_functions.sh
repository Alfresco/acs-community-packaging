extractVariable()
{
    local variablePrefix=$1 commitMessage=$2
    regex="\[$variablePrefix[^\]]*=\K[^\]]*(?=\])" 
    echo $commitMessage | grep -Po $regex
}

echo "common_function.sh loaded"