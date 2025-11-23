pub mod java;

use clap::Subcommand;

#[derive(Subcommand)]
pub enum Commands {
  #[command(subcommand)]
  Java(java::JavaCommands),
}

impl Commands {
  pub fn execute(&self) -> Result<String, Box<dyn std::error::Error>> {
    match self {
      Commands::Java(java_command) => java_command.execute(),
    }
  }
}
