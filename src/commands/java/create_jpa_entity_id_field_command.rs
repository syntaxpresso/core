use std::path::Path;

use crate::{
  commands::java::{
    responses::file_response::FileResponse, services::create_jpa_entity_id_field_service::run,
    treesitter::types::id_field_config::IdFieldConfig,
  },
  common::{response::Response, validators::directory_validator::validate_file_path_within_base},
};

pub fn execute(
  cwd: &Path,
  entity_file_b64_src: &str,
  entity_file_path: &Path,
  field_config: IdFieldConfig,
) -> Response<FileResponse> {
  let cwd_string = cwd.display().to_string();
  let cmd_name = String::from("create-jpa-entity-id-field");
  // Security validation: ensure entity file path is within the cwd
  let file_path_str = entity_file_path.display().to_string();
  if let Err(error_msg) = validate_file_path_within_base(&file_path_str, cwd) {
    return Response::error(
      cmd_name,
      cwd_string,
      format!("Entity file path security validation failed: {}", error_msg),
    );
  }

  match run(cwd, entity_file_b64_src, entity_file_path, field_config) {
    Ok(response) => Response::success(cmd_name, cwd_string, response),
    Err(error_msg) => Response::error(cmd_name, cwd_string, error_msg),
  }
}
