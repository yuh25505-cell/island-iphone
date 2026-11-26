package com.island.phone.core.ui.icons.art

import com.island.phone.core.ui.apps.AppId

object IconArt {
    fun of(app: AppId): List<ArtLayer>? = when (app) {
        AppId.MEET -> ArtXiangyu
        AppId.FORUM -> ArtLuntan
        AppId.ALBUM -> ArtXiangche
        AppId.LINK -> ArtGuanlianzhanghao
        AppId.WEATHER -> ArtTianqi
        AppId.CHAT -> ArtLiaotian
        AppId.PHONE -> ArtDianhua
        AppId.COUPLE -> ArtQinglvkongjian
        AppId.MEMO -> ArtBeiwanglu
        else -> null
    }
}
