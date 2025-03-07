package com.tencent.bk.codecc.task.model.istm;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_itsm_system_info")
public class ItsmSystemInfoEntity extends CommonEntity {
    @Field("system")
    @Indexed(background = true)
    private String system;

    @Field("name_cn")
    private String nameCn;

    @Field("create_ticket_url")
    private String createTicketUrl;

    @Field("create_ticket_header")
    private String createTicketHeader;

    @Field("create_ticket_body")
    private String createTicketBody;

    @Field("get_ticket_status_url")
    private String getTicketStatusUrl;

    @Field("get_ticket_status_header")
    private String getTicketStatusHeader;

    @Field("get_ticket_status_body")
    private String getTicketStatusBody;

    @Field("operate_ticket_url")
    private String operateTicketUrl;

    @Field("operate_ticket_header")
    private String operateTicketHeader;

    @Field("operate_ticket_body")
    private String operateTicketBody;
}
