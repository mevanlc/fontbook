# Publishing FontBook on Google Play

The `Publish to Google Play` GitHub Actions workflow handles repeatable release
builds. A pushed `v*` tag publishes to the internal track. A manual run can
produce an artifact only or publish to the internal, alpha, beta, or production
track.

Google Play account setup, the first bundle upload, store declarations, and the
public privacy-policy URL remain one-time Play Console tasks.

## 1. Confirm the permanent app identity

The application ID is `org.mecresearch.fontbook`, using the domain-owned
`mecresearch.org` namespace. Google Play package names cannot be changed after
the app is created, so use this exact ID in Play Console before the first
upload.

The app targets Android 16 (API 36), the requirement for new phone and tablet
apps submitted on or after August 31, 2026.

## 2. Create the Play Console app

1. Register or sign in to the Google Play developer account and complete its
   identity and contact verification. Full public distribution has a one-time
   USD $25 developer-account registration fee even though FontBook itself is
   free.
2. Create an app named `FontBook`, choose `App`, and choose `Free`.
3. Enroll in Play App Signing and let Google generate and protect the app
   signing key. The separate key created below is only the upload key.

Once an app has been offered for free, Google Play does not allow changing that
same package to a paid download later. In-app products can still be added if
needed.

## 3. Create and back up the upload key

Create the upload key outside this repository. `keytool` prompts for the
passwords and certificate details:

```sh
keytool -genkeypair -v \
  -keystore fontbook-upload.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Keep an encrypted backup of the keystore and its passwords. The keystore is
ignored by Git and must never be committed.

Configure the repository secrets without printing their values:

```sh
openssl base64 -A -in fontbook-upload.jks | gh secret set PLAY_UPLOAD_KEYSTORE_BASE64
gh secret set PLAY_UPLOAD_STORE_PASSWORD
gh secret set PLAY_UPLOAD_KEY_ALIAS
gh secret set PLAY_UPLOAD_KEY_PASSWORD
```

Use `upload` for `PLAY_UPLOAD_KEY_ALIAS` if the command above was used as-is.

## 4. Bootstrap the first bundle

The Google Play Developer API cannot bootstrap a package that has never had a
bundle uploaded in Play Console.

1. In GitHub, open **Actions > Publish to Google Play > Run workflow**.
2. Choose `artifact-only` and run it.
3. Download the signed `.aab` from the completed workflow's artifacts.
4. In Play Console, create an internal-testing release and manually upload that
   bundle. Complete the Play App Signing prompts.

The artifact-only job does not need Google Cloud authentication. It does need
the four upload-key secrets from the previous section.

## 5. Configure keyless Play API authentication

Use Google Cloud Workload Identity Federation so GitHub receives short-lived
credentials instead of storing a service-account JSON key.

1. Create or select a Google Cloud project and enable the **Google Play Android
   Developer API**.
2. Create a service account with no project-wide role.
3. Create a workload identity pool and GitHub OIDC provider restricted to the
   `mevanlc/fontbook` repository.
4. Allow that repository identity to impersonate the service account with
   `roles/iam.workloadIdentityUser`.
5. In Play Console, open **Users and permissions**, invite the service-account
   email, and grant it app-level access to FontBook. Grant testing-release
   permissions, plus production-release permission only if the workflow should
   publish production releases.
6. Save the provider resource name and service-account email as GitHub
   repository variables:

```sh
gh variable set GCP_WORKLOAD_IDENTITY_PROVIDER
gh variable set GCP_PLAY_SERVICE_ACCOUNT
```

The provider value has the form
`projects/NUMBER/locations/global/workloadIdentityPools/POOL/providers/PROVIDER`.
The service-account value has the form `NAME@PROJECT.iam.gserviceaccount.com`.

Create GitHub environments named `google-play-internal`, `google-play-alpha`,
`google-play-beta`, and `google-play-production`. Add required reviewers to the
production environment so production publication always has an approval gate.

## 6. Complete the store listing and app-content forms

Draft listing copy and graphics are under `distribution/store-listing/` and
`distribution/store-assets/`. Four real API-36 emulator screenshots are
included at 1080 x 1920.

Suggested Play Console choices, based on the current source:

- Category: **Art & Design**
- Ads: **No**
- App access: all functionality is available without an account or login
- Data safety: no user data is collected or shared
- Account creation: none
- Target audience: choose the ages actually intended; do not select under 13
  unless the app is intentionally directed to children

Every app still needs a public privacy-policy URL. A ready-to-host page is at
`distribution/privacy-policy/index.html`; publish it at a stable, public,
non-geofenced HTML URL such as `https://mecresearch.org/fontbook/privacy/`, then
enter that URL in Play Console. The same policy is available inside the app at
**Settings > Privacy policy**.

Review the declarations again whenever dependencies, permissions, analytics,
ads, accounts, or network behavior change.

## 7. Release flow

For an internal release, either manually run the workflow with `internal`, or
push a version tag:

```sh
git tag v1.0.0
git push origin v1.0.0
```

Tags always go to the internal track. Use a manual workflow run to publish to a
different track. The workflow derives a monotonically increasing version code
from the GitHub run number and uses the tag (without `v`) as the version name.
Update `distribution/whatsnew/whatsnew-en-US` before each release.

New personal developer accounts created after November 13, 2023 must complete a
closed test with at least 12 continuously opted-in testers for 14 days before
requesting production access. The `alpha` destination is the conventional
default closed-testing track; confirm its track name in Play Console if custom
closed tracks are created.
