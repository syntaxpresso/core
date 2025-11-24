use std::path::Path;

use crate::{
  commands::java::responses::file_response::FileResponse,
  commands::java::services::create_jpa_entity_basic_field_service::run,
  commands::java::treesitter::types::basic_field_config::BasicFieldConfig,
  common::response::Response,
};

pub fn execute(
  cwd: &Path,
  entity_file_b64_src: &str,
  entity_file_path: &Path,
  field_config: &BasicFieldConfig,
) -> Response<FileResponse> {
  let cwd_string = cwd.display().to_string();
  let cmd_name = String::from("create-jpa-entity-basic-field");

  match run(entity_file_b64_src, entity_file_path, field_config, cwd) {
    Ok(response) => Response::success(cmd_name, cwd_string, response),
    Err(error_msg) => Response::error(cmd_name, cwd_string, error_msg),
  }
}
