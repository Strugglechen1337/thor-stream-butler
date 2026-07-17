package de.thorstream.butler.data.common

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import de.thorstream.butler.core.common.StringProvider
import javax.inject.Inject
import javax.inject.Singleton

/** Default [StringProvider] backed by Android resources. */
@Singleton
class AndroidStringProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : StringProvider {
    override fun get(resId: Int, vararg args: Any): String =
        if (args.isEmpty()) context.getString(resId) else context.getString(resId, *args)
}
