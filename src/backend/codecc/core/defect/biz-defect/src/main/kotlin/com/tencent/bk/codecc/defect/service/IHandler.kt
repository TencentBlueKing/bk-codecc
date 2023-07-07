package com.tencent.bk.codecc.defect.service

import com.tencent.bk.codecc.defect.pojo.HandlerDTO

interface IHandler {
    fun handler(handlerDTO: HandlerDTO)
}
