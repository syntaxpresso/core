#![allow(dead_code)]

use serde::Serialize;

use crate::commands::java::responses::file_response::FileResponse;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CreateJPARepositoryResponse {
  pub id_field_found: bool,
  pub superclass_type: Option<String>,
  pub repository: Option<FileResponse>,
}
