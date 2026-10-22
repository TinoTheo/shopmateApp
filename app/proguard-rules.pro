# ShopMate release R8 rules (full mode, AGP default).
#
# Most libraries ship their own consumer rules and need nothing here:
#   - kotlinx.serialization keeps the serializers of every @Serializable class — ShopMate's
#     sync rows (data/remote/dto) and supabase-kt's own models (UserSession, …) alike.
#   - Hilt, Room, WorkManager, Compose and Ktor (ktor-utils) ship theirs.
# Only add a rule below with a comment saying which crash or R8 error it answers.

# Readable crash stack traces from the field: keep line numbers, hide the source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
