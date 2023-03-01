package com.mycz.krpc.sample.request;


import lombok.*;

@Getter
@Setter
public abstract class BaseRequest {

    private BaseRequestHeader header;

}
