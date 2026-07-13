#!/usr/bin/env bash
# Generate the PKCS12 keystore that signs BOTH the release APK and the F-Droid repo
# index. Run once, keep the file safe (losing it means no more signed updates), then
# upload it + passwords as GitHub Actions secrets. See docs/FDROID.md.
set -euo pipefail

# keytool ships with the JDK, which in this project is only on PATH inside nix-shell.
# NOTE: macOS ships a /usr/bin/keytool *stub* that exists but fails without a JRE,
# so test that keytool actually runs rather than just that it's on PATH.
if ! keytool -help >/dev/null 2>&1; then
  if [ -z "${_MK_IN_NIX:-}" ] && command -v nix-shell >/dev/null 2>&1; then
    echo "Working keytool not found; re-running inside nix-shell..." >&2
    cd "$(dirname "$0")/.."
    exec env _MK_IN_NIX=1 nix-shell --run "scripts/make-keystore.sh $*"
  fi
  echo "Need a working JDK 'keytool'. Install a JDK or run inside nix-shell." >&2
  exit 1
fi

ALIAS="${1:-gymlogger}"     # must match repo_keyalias in fdroid/config.yml
OUT="${2:-gymlogger.p12}"

if [ -f "$OUT" ]; then
  echo "Refusing to overwrite existing $OUT" >&2
  exit 1
fi

read -rsp "Keystore password (used for both store and key): " PASS; echo
if [ -z "$PASS" ]; then echo "Empty password, aborting." >&2; exit 1; fi

# PKCS12 requires the store and key passwords to be identical.
keytool -genkeypair \
  -keystore "$OUT" -storetype PKCS12 \
  -alias "$ALIAS" \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS" \
  -dname "CN=GymLogger, OU=GymLogger, O=GymLogger"

echo
echo "Created $OUT (alias: $ALIAS)"
echo
echo "Add these repo secrets under Settings -> Secrets and variables -> Actions:"
echo "  SIGNING_KEYSTORE_BASE64  ->  base64 -i \"$OUT\" | pbcopy   (paste as the value)"
echo "  KEYSTORE_PASSWORD        ->  the password you just entered"
echo "  KEY_PASSWORD             ->  the same password"
echo "  KEY_ALIAS                ->  $ALIAS"
