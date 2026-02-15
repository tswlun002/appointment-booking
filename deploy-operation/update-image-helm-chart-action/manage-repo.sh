#!/bin/bash
echo "# When run manage-repo, format must be: GITHUB_ACTION_PATH/manage-repo.sh <repo_args1>  <module_args2> <image_args3> <env_args4>"

number_of_args=3
# Check all arguments are provided
if [ "$#" -lt ${number_of_args} ]; then
  echo "Warning! Got $# but at least ${number_of_args} arguments must be provided."
  exit 1
fi

# Get all inputs
repository="$1"
module="$2"
image="$3"
environment="$4"

echo "========== INPUTS ================"
echo "    repository=${repository}  "
echo "    module=${module}          "
echo "    environment=${environment}"
echo "    image=${image}            "
echo "=========================="

# Go to project
cd "${repository}/" || { echo "Error: Repository ${repository} not found."; exit 1; }

# In Helm
helm_directory="helm"
if [[ -d "${helm_directory}" ]]; then
  cd "${helm_directory}"

  echo "__________ Helm Chart content _________"
  ls -alrth

  if [[ -d ${module} ]]; then
    cd ${module}
  else
    echo "Error: Module:${module} does not exist in the deployment repo"
    exit 1
  fi

  echo "___________ Update helm with new image in module: ${module}"

  values_file=values.yaml
  # Update values files if there is environmental values file like values-environment.yaml
  if [ -f "values-${environment}.yaml" ]; then
    values_file=values-${environment}.yaml
  fi

  if [[ -f ${values_file} ]]; then
    echo "______________ Current values of ${values_file} "
    echo "=============================================================================="
    cat "${values_file}"
    image_tag="${image#*:}"
    image_repository="${image%%:*}"
    echo "Image tag: ${image_tag}"
    echo "Image repository: ${image_repository}"
    echo "yq version: $(yq --version)"
    yq -i '.active = true' ${values_file}
    yq -i ".image.tag = \"${image_tag}\"" ${values_file}
    yq -i ".image.repository = \"${image_repository}\"" ${values_file}
    echo "Updated values of ${values_file}"
    echo "=============================================================================="
    cat "${values_file}"
  else
    echo "Warning: ${values_file} DOES NOT EXIST!!!"
  fi
  cd ../
else
  echo "ERROR!: helm directory is found, INVALID DEPLOYMENT!!!"
  exit 1
fi
