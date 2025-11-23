use std::path::PathBuf;

use clap::Subcommand;

#[cfg(feature = "ui")]
use crate::common::ui::runner::run_ui_command;

#[cfg(feature = "ui")]
use crate::commands::java::ui::{
  create_entity_field::CreateEntityFieldForm,
  create_entity_relationship::CreateEntityRelationshipForm, create_java_file::CreateJavaFileForm,
  create_jpa_entity::CreateJpaEntityForm, create_jpa_repository::CreateJpaRepositoryForm,
};

use crate::commands::java::{
  create_java_file_command, create_jpa_entity_basic_field_command, create_jpa_entity_command,
  create_jpa_entity_enum_field_command, create_jpa_entity_id_field_command,
  create_jpa_many_to_one_relationship_command, create_jpa_one_to_one_relationship_command,
  create_jpa_repository_command, get_all_jpa_entities_command, get_all_jpa_mapped_superclasses,
  get_all_packages_command, get_java_basic_types_command, get_java_files_command,
  get_jpa_entity_info_command,
  treesitter::types::{
    basic_field_config::BasicFieldConfig, cascade_type::CascadeType,
    collection_type::CollectionType, enum_field_config::EnumFieldConfig, fetch_type::FetchType,
    id_field_config::IdFieldConfig, java_basic_types::JavaBasicType, java_enum_type::JavaEnumType,
    java_field_temporal::JavaFieldTemporal, java_field_time_zone_storage::JavaFieldTimeZoneStorage,
    java_file_type::JavaFileType, java_id_generation::JavaIdGeneration,
    java_id_generation_type::JavaIdGenerationType,
    java_source_directory_type::JavaSourceDirectoryType,
    many_to_one_field_config::ManyToOneFieldConfig, mapping_type::MappingType,
    one_to_one_field_config::OneToOneFieldConfig, other_type::OtherType,
  },
  validators::{
    directory_validator::validate_directory_unrestricted,
    java_class_name_validator::validate_java_class_name,
    package_name_validator::validate_package_name,
  },
};

#[derive(Subcommand)]
pub enum JavaCommands {
  // ============ UI Commands (with --features ui) ============
  #[cfg(feature = "ui")]
  #[command(name = "create-java-file-ui")]
  CreateJavaFileUi {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,
  },
  #[cfg(feature = "ui")]
  #[command(name = "create-jpa-entity-ui")]
  CreateJpaEntityUi {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,
  },
  #[cfg(feature = "ui")]
  #[command(name = "create-jpa-entity-basic-field-ui")]
  CreateJpaEntityBasicFieldUi {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,

    #[arg(long, required = true)]
    entity_file_b64_src: String,

    #[arg(long, required = true)]
    entity_file_path: PathBuf,
  },
  #[cfg(feature = "ui")]
  #[command(name = "create-jpa-one-to-one-relationship-ui")]
  CreateJpaOneToOneRelationshipUi {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,

    #[arg(long, required = true)]
    entity_file_b64_src: String,

    #[arg(long, required = true)]
    entity_file_path: PathBuf,
  },
  #[cfg(feature = "ui")]
  #[command(name = "create-jpa-repository-ui")]
  CreateJpaRepositoryUi {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,

    #[arg(long, required = true)]
    entity_file_b64_src: String,

    #[arg(long, required = true)]
    entity_file_path: PathBuf,
  },

  // ============ CLI Commands (always available) ============
  GetAllJPAEntities {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,
  },
  GetAllJPAMappedSuperclasses {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,
  },
  GetJPAEntityInfo {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,

    #[arg(long, required = false)]
    entity_file_path: Option<PathBuf>,

    #[arg(long, required = false)]
    b64_source_code: Option<String>,
  },
  GetAllPackages {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,

    #[arg(long, default_value = "main")]
    source_directory: JavaSourceDirectoryType,
  },
  GetJavaBasicTypes {
    #[arg(long, default_value = "all-types")]
    basic_type_kind: JavaBasicType,
  },
  GetJavaFiles {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,

    #[arg(long, required = true)]
    file_type: JavaFileType,
  },
  CreateJavaFile {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,

    #[arg(long, value_parser = validate_package_name, required = true)]
    package_name: String,

    #[arg(long, value_parser = validate_java_class_name, required = true)]
    file_name: String,

    #[arg(long, required = true)]
    file_type: JavaFileType,

    #[arg(long, default_value = "main")]
    source_directory: JavaSourceDirectoryType,
  },
  CreateJPAEntity {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,

    #[arg(long, value_parser = validate_package_name, required = true)]
    package_name: String,

    #[arg(long, value_parser = validate_java_class_name, required = true)]
    file_name: String,

    #[arg(long, required = false)]
    superclass_type: Option<String>,

    #[arg(long, required = false)]
    superclass_package_name: Option<String>,
  },
  CreateJPARepository {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,

    #[arg(long, required = true)]
    entity_file_b64_src: String,

    #[arg(long, required = true)]
    entity_file_path: PathBuf,

    #[arg(long, required = false)]
    b64_superclass_source: Option<String>,
  },
  CreateJPAEntityBasicField {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,

    #[arg(long, required = true)]
    entity_file_path: PathBuf,

    #[arg(long, required = true)]
    entity_file_b64_src: String,

    #[arg(long, required = true)]
    field_name: String,

    #[arg(long, required = true)]
    field_type: String,

    #[arg(long, required = false)]
    field_type_package_name: Option<String>,

    #[arg(long, required = false)]
    field_length: Option<u16>,

    #[arg(long, required = false)]
    field_precision: Option<u16>,

    #[arg(long, required = false)]
    field_scale: Option<u16>,

    #[arg(long, required = false)]
    field_temporal: Option<JavaFieldTemporal>,

    #[arg(long, required = false)]
    field_timezone_storage: Option<JavaFieldTimeZoneStorage>,

    #[arg(long)]
    field_unique: bool,

    #[arg(long)]
    field_nullable: bool,

    #[arg(long)]
    field_large_object: bool,
  },
  CreateJPAEntityIdField {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,

    #[arg(long, required = true)]
    entity_file_b64_src: String,

    #[arg(long, required = true)]
    entity_file_path: PathBuf,

    #[arg(long, required = true)]
    field_name: String,

    #[arg(long, required = true)]
    field_type: String,

    #[arg(long, required = false)]
    field_type_package_name: Option<String>,

    #[arg(long, required = true)]
    field_id_generation: JavaIdGeneration,

    #[arg(long, required = true)]
    field_id_generation_type: JavaIdGenerationType,

    #[arg(long, required = false)]
    field_generator_name: Option<String>,

    #[arg(long, required = false)]
    field_sequence_name: Option<String>,

    #[arg(long, required = false)]
    field_initial_value: Option<i64>,

    #[arg(long, required = false)]
    field_allocation_size: Option<i64>,

    #[arg(long)]
    field_nullable: bool,
  },
  CreateJPAEntityEnumField {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,

    #[arg(long, required = true)]
    entity_file_b64_src: String,

    #[arg(long, required = true)]
    entity_file_path: PathBuf,

    #[arg(long, required = true)]
    field_name: String,

    #[arg(long, required = true)]
    enum_type: String,

    #[arg(long, required = true)]
    enum_package_name: String,

    #[arg(long, required = true)]
    enum_type_storage: JavaEnumType,

    #[arg(long, required = false)]
    field_length: Option<u16>,

    #[arg(long)]
    field_nullable: bool,

    #[arg(long)]
    field_unique: bool,
  },
  CreateJPAOneToOneRelationship {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,

    #[arg(long, required = true)]
    owning_side_entity_file_b64_src: String,

    #[arg(long, required = true)]
    owning_side_entity_file_path: PathBuf,

    #[arg(long, required = true)]
    owning_side_field_name: String,

    #[arg(long, required = true)]
    inverse_side_field_name: String,

    #[arg(long, required = true)]
    inverse_field_type: String,

    #[arg(long, required = false)]
    mapping_type: Option<MappingType>,

    #[arg(long, required = false)]
    owning_side_cascades: Vec<CascadeType>,

    #[arg(long, required = false)]
    inverse_side_cascades: Vec<CascadeType>,

    #[arg(long, required = false)]
    owning_side_other: Vec<OtherType>,

    #[arg(long, required = false)]
    inverse_side_other: Vec<OtherType>,
  },
  CreateJPAManyToOneRelationship {
    #[arg(long, value_parser = validate_directory_unrestricted, required = true)]
    cwd: PathBuf,

    #[arg(long, required = true)]
    owning_side_entity_file_b64_src: String,

    #[arg(long, required = true)]
    owning_side_entity_file_path: PathBuf,

    #[arg(long, required = true)]
    owning_side_field_name: String,

    #[arg(long, required = true)]
    inverse_side_field_name: String,

    #[arg(long, required = true)]
    inverse_field_type: String,

    #[arg(long, required = true)]
    fetch_type: FetchType,

    #[arg(long, required = true)]
    collection_type: CollectionType,

    #[arg(long, required = false)]
    mapping_type: Option<MappingType>,

    #[arg(long, required = false)]
    owning_side_cascades: Vec<CascadeType>,

    #[arg(long, required = false)]
    inverse_side_cascades: Vec<CascadeType>,

    #[arg(long, required = false)]
    owning_side_other: Vec<OtherType>,

    #[arg(long, required = false)]
    inverse_side_other: Vec<OtherType>,
  },
}

impl JavaCommands {
  pub fn execute(&self) -> Result<String, Box<dyn std::error::Error>> {
    match self {
      // ============ UI Commands ============
      #[cfg(feature = "ui")]
      JavaCommands::CreateJavaFileUi { cwd } => {
        let form = CreateJavaFileForm::new(cwd.clone());
        run_ui_command(form)?;
        Ok(String::new()) // UI commands don't return JSON
      }
      #[cfg(feature = "ui")]
      JavaCommands::CreateJpaEntityUi { cwd } => {
        let form = CreateJpaEntityForm::new(cwd.clone());
        run_ui_command(form)?;
        Ok(String::new())
      }
      #[cfg(feature = "ui")]
      JavaCommands::CreateJpaEntityBasicFieldUi { cwd, entity_file_b64_src, entity_file_path } => {
        let form = CreateEntityFieldForm::new(
          cwd.clone(),
          entity_file_b64_src.clone(),
          entity_file_path.clone(),
        );
        run_ui_command(form)?;
        Ok(String::new())
      }
      #[cfg(feature = "ui")]
      JavaCommands::CreateJpaOneToOneRelationshipUi { cwd, entity_file_b64_src, entity_file_path } => {
        let form = CreateEntityRelationshipForm::new(
          cwd.clone(),
          entity_file_b64_src.clone(),
          entity_file_path.clone(),
        );
        run_ui_command(form)?;
        Ok(String::new())
      }
      #[cfg(feature = "ui")]
      JavaCommands::CreateJpaRepositoryUi { cwd, entity_file_b64_src, entity_file_path } => {
        let form = CreateJpaRepositoryForm::new(
          cwd.clone(),
          entity_file_b64_src.clone(),
          entity_file_path.clone(),
        );
        run_ui_command(form)?;
        Ok(String::new())
      }

      // ============ CLI Commands ============
      JavaCommands::GetAllJPAEntities { cwd } => {
        let response = get_all_jpa_entities_command::execute(cwd.as_path());
        response.to_json_pretty().map_err(|e| e.into())
      }
      JavaCommands::GetAllJPAMappedSuperclasses { cwd } => {
        let response = get_all_jpa_mapped_superclasses::execute(cwd.as_path());
        response.to_json_pretty().map_err(|e| e.into())
      }
      JavaCommands::GetJPAEntityInfo { cwd, entity_file_path, b64_source_code } => {
        let response = get_jpa_entity_info_command::execute(
          cwd.as_path(),
          entity_file_path.as_deref(),
          b64_source_code.as_deref(),
        );
        response.to_json_pretty().map_err(|e| e.into())
      }
      JavaCommands::GetAllPackages { cwd, source_directory } => {
        let response = get_all_packages_command::execute(cwd.as_path(), source_directory);
        response.to_json_pretty().map_err(|e| e.into())
      }
      JavaCommands::GetJavaBasicTypes { basic_type_kind } => {
        let response = get_java_basic_types_command::execute(basic_type_kind);
        response.to_json_pretty().map_err(|e| e.into())
      }
      JavaCommands::GetJavaFiles { cwd, file_type } => {
        let response = get_java_files_command::execute(cwd.as_path(), file_type);
        response.to_json_pretty().map_err(|e| e.into())
      }
      JavaCommands::CreateJavaFile {
        cwd,
        package_name,
        file_name,
        file_type,
        source_directory,
      } => {
        let response = create_java_file_command::execute(
          cwd.as_path(),
          package_name,
          file_name,
          file_type,
          source_directory,
        );
        response.to_json_pretty().map_err(|e| e.into())
      }
      JavaCommands::CreateJPAEntity {
        cwd,
        package_name,
        file_name,
        superclass_type,
        superclass_package_name,
      } => {
        let response = create_jpa_entity_command::execute(
          cwd.as_path(),
          package_name,
          file_name,
          superclass_type.as_deref(),
          superclass_package_name.as_deref(),
        );
        response.to_json_pretty().map_err(|e| e.into())
      }
      JavaCommands::CreateJPARepository {
        cwd,
        entity_file_b64_src,
        entity_file_path,
        b64_superclass_source,
      } => {
        let response = create_jpa_repository_command::execute(
          cwd.as_path(),
          entity_file_b64_src,
          entity_file_path.as_path(),
          b64_superclass_source.as_deref(),
        );
        response.to_json_pretty().map_err(|e| e.into())
      }
      JavaCommands::CreateJPAEntityBasicField {
        cwd,
        entity_file_path,
        entity_file_b64_src,
        field_name,
        field_type,
        field_type_package_name,
        field_length,
        field_precision,
        field_scale,
        field_temporal,
        field_timezone_storage,
        field_unique,
        field_nullable,
        field_large_object,
      } => {
        let field_config = BasicFieldConfig {
          field_name: field_name.clone(),
          field_type: field_type.clone(),
          field_type_package_name: field_type_package_name.clone(),
          field_length: *field_length,
          field_precision: *field_precision,
          field_scale: *field_scale,
          field_temporal: field_temporal.clone(),
          field_timezone_storage: field_timezone_storage.clone(),
          field_unique: *field_unique,
          field_nullable: *field_nullable,
          field_large_object: *field_large_object,
        };
        let response = create_jpa_entity_basic_field_command::execute(
          cwd.as_path(),
          entity_file_b64_src,
          entity_file_path.as_path(),
          &field_config,
        );
        response.to_json_pretty().map_err(|e| e.into())
      }
      JavaCommands::CreateJPAEntityIdField {
        cwd,
        entity_file_b64_src,
        entity_file_path,
        field_name,
        field_type,
        field_type_package_name,
        field_id_generation,
        field_id_generation_type,
        field_generator_name,
        field_sequence_name,
        field_initial_value,
        field_allocation_size,
        field_nullable,
      } => {
        let field_config = IdFieldConfig {
          field_name: field_name.clone(),
          field_type: field_type.clone(),
          field_type_package_name: field_type_package_name.clone(),
          field_id_generation: field_id_generation.clone(),
          field_id_generation_type: field_id_generation_type.clone(),
          field_generator_name: field_generator_name.clone(),
          field_sequence_name: field_sequence_name.clone(),
          field_initial_value: *field_initial_value,
          field_allocation_size: *field_allocation_size,
          field_nullable: *field_nullable,
        };
        let response = create_jpa_entity_id_field_command::execute(
          cwd.as_path(),
          entity_file_b64_src,
          entity_file_path.as_path(),
          field_config,
        );
        response.to_json_pretty().map_err(|e| e.into())
      }
      JavaCommands::CreateJPAEntityEnumField {
        cwd,
        entity_file_b64_src,
        entity_file_path,
        field_name,
        enum_type,
        enum_package_name,
        enum_type_storage,
        field_length,
        field_nullable,
        field_unique,
      } => {
        let field_config = EnumFieldConfig {
          field_name: field_name.clone(),
          enum_type: enum_type.clone(),
          enum_package_name: enum_package_name.clone(),
          enum_type_storage: enum_type_storage.clone(),
          field_length: *field_length,
          field_nullable: *field_nullable,
          field_unique: *field_unique,
        };
        let response = create_jpa_entity_enum_field_command::execute(
          cwd.as_path(),
          entity_file_b64_src,
          entity_file_path.as_path(),
          field_config,
        );
        response.to_json_pretty().map_err(|e| e.into())
      }
      JavaCommands::CreateJPAOneToOneRelationship {
        cwd,
        owning_side_entity_file_b64_src,
        owning_side_entity_file_path,
        owning_side_field_name,
        inverse_side_field_name,
        inverse_field_type,
        mapping_type,
        owning_side_cascades,
        inverse_side_cascades,
        owning_side_other,
        inverse_side_other,
      } => {
        let config = OneToOneFieldConfig {
          inverse_field_type: inverse_field_type.clone(),
          mapping_type: mapping_type.clone(),
          owning_side_cascades: owning_side_cascades.clone(),
          inverse_side_cascades: inverse_side_cascades.clone(),
          owning_side_other: owning_side_other.clone(),
          inverse_side_other: inverse_side_other.clone(),
        };
        let response = create_jpa_one_to_one_relationship_command::execute(
          cwd.as_path(),
          owning_side_entity_file_b64_src,
          owning_side_entity_file_path.as_path(),
          owning_side_field_name.clone(),
          inverse_side_field_name.clone(),
          config,
        );
        response.to_json_pretty().map_err(|e| e.into())
      }
      JavaCommands::CreateJPAManyToOneRelationship {
        cwd,
        owning_side_entity_file_b64_src,
        owning_side_entity_file_path,
        owning_side_field_name,
        inverse_side_field_name,
        inverse_field_type,
        fetch_type,
        collection_type,
        mapping_type,
        owning_side_cascades,
        inverse_side_cascades,
        owning_side_other,
        inverse_side_other,
      } => {
        let config = ManyToOneFieldConfig {
          inverse_field_type: inverse_field_type.clone(),
          fetch_type: fetch_type.clone(),
          collection_type: collection_type.clone(),
          mapping_type: mapping_type.clone(),
          owning_side_cascades: owning_side_cascades.clone(),
          inverse_side_cascades: inverse_side_cascades.clone(),
          owning_side_other: owning_side_other.clone(),
          inverse_side_other: inverse_side_other.clone(),
        };
        let response = create_jpa_many_to_one_relationship_command::execute(
          cwd.as_path(),
          owning_side_entity_file_b64_src,
          owning_side_entity_file_path.as_path(),
          owning_side_field_name.clone(),
          inverse_side_field_name.clone(),
          config,
        );
        response.to_json_pretty().map_err(|e| e.into())
      }
    }
  }
}

