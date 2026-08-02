package io.github.nexalloy.morphe.youtube.layout.shortsnoresume

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.accessFlags
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.fingerprint
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.opcodes
import io.github.nexalloy.morphe.parameters
import io.github.nexalloy.morphe.returns

val userWasInShortsFingerprint = findMethodDirect {
    runCatching {
        fingerprint {
            returns("V")
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            parameters("Ljava/lang/Object;")
            strings("userIsInShorts: ")
        }
    }.getOrElse {
        findMethod {
            matcher {
                returns("V")
                accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
                parameters("Ljava/lang/Object;")
                opcodes(
                    Opcode.INVOKE_INTERFACE, // userWasInShortsProtoStoreProvider
                    Opcode.MOVE_RESULT_OBJECT,
                    Opcode.CHECK_CAST,
                    Opcode.NEW_INSTANCE,
                    Opcode.INVOKE_DIRECT, // userWasInShortsBuilder
                    Opcode.INVOKE_INTERFACE,
                    Opcode.RETURN_VOID,
                )
            }
        }.findMethod {
            matcher {
                opcodes(
                    Opcode.CHECK_CAST, // p1, Ljava/lang/Boolean; // userIsInShorts
                    Opcode.INVOKE_VIRTUAL, // Ljava/lang/Boolean;->booleanValue()Z
                    Opcode.MOVE_RESULT,
                    Opcode.IGET_OBJECT,
                    Opcode.MOVE_OBJECT,
                    Opcode.CHECK_CAST,
                    Opcode.IGET_OBJECT,
                    Opcode.INVOKE_INTERFACE, // userWasInShortsProtoStoreProvider
                )
            }
        }.single()
    }
}

/**
 * 18.15.40+
 *
 * 21.30.209: the async refactor removed the zero-param config getter.
 * The boolean "should resume Shorts" decision now lives in a PUBLIC FINAL
 * `(Lsgd;)Z` method that reads feature flag literal 45358360L via `Lyba;.y(JZ)Z`
 * plus the `UserWasInShorts` proto (`Lsgd;.c` = userWasInShorts). The
 * literal + Z return uniquely identifies it among the three literal-45358360
 * methods (the other two return void). Forcing this to false fixes the read
 * path (AppStartupBehaviour proto + the `Lsgb` resume getter).
 */
internal object UserWasInShortsConfigFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    filters = listOf(
        literal(45358360L)
    ),
)

/**
 * 21.30.209 action layer.
 *
 * The async refactor split the old zero-param getter into a read path
 * (see [UserWasInShortsConfigFingerprint]) and a write path. The write path is
 * `Lnqv;.f:()V` (PUBLIC FINAL, zero params, void) — invoked by
 * `DefaultStartupPaneResolver` to resolve the cached Shorts first command. It
 * reads the same feature flag literal 45358360L **directly** via
 * `Lyba;.y(JZ)Z` (bypassing the evaluate method) and, when true, flips the
 * resume flags (`Lsgb;.f` / `Lsgb;.g` AtomicBoolean) and builds the startup
 * behaviour. Skipping this method prevents Shorts from resuming on startup.
 *
 * Uniquely identified by definingClass + PUBLIC FINAL + zero params + void
 * return + literal (the other literal-45358360 readers are `audt.o:(Lsgd;)Z`
 * and the two-param `afls.a:(Lbcil;Lberw;)V`).
 */
internal object ShortsResumingOnStartupActionFingerprint : Fingerprint(
    definingClass = "Lnqv;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        literal(45358360L)
    ),
)
