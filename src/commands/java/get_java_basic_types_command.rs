use crate::{
  commands::java::responses::basic_java_type_response::JavaBasicTypeResponse,
  commands::java::services::get_java_basic_types_service::run,
  commands::java::treesitter::types::java_basic_types::JavaBasicType, common::response::Response,
};

pub fn execute(basic_type_kind: &JavaBasicType) -> Response<Vec<JavaBasicTypeResponse>> {
  let cmd_name = String::from("get-java-basic-types");
  match run(basic_type_kind) {
    Ok(types) => Response::success(cmd_name, String::from("N/A"), types),
    Err(error_msg) => Response::error(cmd_name, String::from("N/A"), error_msg),
  }
}
