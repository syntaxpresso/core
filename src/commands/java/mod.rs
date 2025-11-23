// Command modules
pub mod create_java_file_command;
pub mod create_jpa_entity_basic_field_command;
pub mod create_jpa_entity_command;
pub mod create_jpa_entity_enum_field_command;
pub mod create_jpa_entity_id_field_command;
pub mod create_jpa_many_to_one_relationship_command;
pub mod create_jpa_one_to_one_relationship_command;
pub mod create_jpa_repository_command;
pub mod get_all_jpa_entities_command;
pub mod get_all_jpa_mapped_superclasses;
pub mod get_all_packages_command;
pub mod get_java_basic_types_command;
pub mod get_java_files_command;
pub mod get_jpa_entity_info_command;

// Supporting modules
pub mod commands;
pub mod responses;
pub mod services;
pub mod treesitter;
pub mod validators;

#[cfg(feature = "ui")]
pub mod ui;

// Re-export commands for easier access
pub use commands::JavaCommands;
