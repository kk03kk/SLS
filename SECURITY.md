# Security

Policy artifacts and training checkpoints are executable-model inputs. Only load
artifacts produced by a trusted SLS training run and verify the SHA-256 recorded
in its training bundle. Policy artifacts use restricted weights-only loading;
training checkpoints remain trusted local recovery files.

Do not report vulnerabilities in public issues. Contact the repository owner
through the private contact method on the GitHub profile and include a minimal
reproducer, affected commit, and impact assessment.
