use std::collections::HashSet;

use serde::Serialize;

use crate::commands::java::responses::package_response::PackageResponse;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct GetPackagesResponse {
  pub packages: HashSet<PackageResponse>,
  pub packages_count: usize,
  pub root_package_name: Option<String>,
}
