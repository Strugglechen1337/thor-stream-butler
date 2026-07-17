# Security policy

## Supported version

During the MVP phase, only the current version on the default branch is supported.

## Reporting a vulnerability

Please report security vulnerabilities privately through
[GitHub Security Advisories](https://github.com/Strugglechen1337/thor-stream-butler/security/advisories/new).
Do not open a public issue for a vulnerability.

A useful report includes:

- affected version or commit ID
- reproducible steps
- expected and actual impact
- Android version and device class
- a possible mitigation, if known

Remove IP addresses, SSIDs, MAC addresses, hostnames, account data, and other
personal information from reports, logs, and attachments.

## Security principles

- No credentials or accounts in the MVP
- No cloud, tracking, analytics, or telemetry services
- Local-network permissions only for explicit host actions
- No hidden Android APIs and no root access
- No diagnostic data stored outside the private app sandbox
- Timeouts and cancellation for network operations
- No sensitive network identifiers in logs
