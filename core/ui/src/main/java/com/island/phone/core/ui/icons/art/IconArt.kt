package com.island.phone.core.ui.icons.art

import com.island.phone.core.ui.apps.AppId

object IconArt {
    fun of(app: AppId): List<ArtLayer>? = when (app) {
        AppId.AUTH -> ArtDengluxitong
        AppId.CHAT -> ArtLiaotian
        AppId.APPEARANCE -> ArtWaiguan
        AppId.GROWTH -> ArtYunian
        AppId.ALBUM -> ArtXiangche
        AppId.CALENDAR -> ArtRili
        AppId.MEMO -> ArtBeiwanglu
        AppId.WEATHER -> ArtTianqi
        AppId.CLOCK -> ArtShizhong
        AppId.SETTINGS -> ArtShezhi
        AppId.PHONE -> ArtDianhua
        AppId.COUNTDOWN -> ArtDaoshuri
        AppId.WORLDBOOK -> ArtShijieshu
        AppId.MUSIC -> ArtYinyue
        AppId.MEET -> ArtXiangyu
        AppId.LINK -> ArtGuanlianzhanghao
        AppId.FORUM -> ArtLuntan
        AppId.NOVEL -> ArtWenxuecheng
        AppId.COUPLE -> ArtQinglvkongjian
        AppId.PRESET -> ArtYushe
        AppId.SIDELINE -> ArtFanwaijian
        AppId.SHOP -> ArtMeituan
        AppId.DREAM -> ArtXichi
    }
}
