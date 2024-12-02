package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import mega.privacy.android.app.presentation.psa.legacy.LegacyPsaHandler
import mega.privacy.android.app.presentation.psa.legacy.PsaHandler
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.presentation.security.PasscodeFacade

/**
 * Activity module
 *
 * Provides any dependencies needed by the hosting activities
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class ActivityModule {

    /**
     * Bind passcode check
     *
     * @param implementation
     */
    @Binds
    abstract fun bindPasscodeCheck(implementation: PasscodeFacade): PasscodeCheck

    /**
     * Bind psa handler
     *
     * @param implementation
     */
    @Binds
    abstract fun bindPsaHandler(implementation: LegacyPsaHandler): PsaHandler

}