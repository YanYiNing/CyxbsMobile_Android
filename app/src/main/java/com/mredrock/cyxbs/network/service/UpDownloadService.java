package com.mredrock.cyxbs.network.service;


import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Url;
import rx.Observable;

/**
 * Created by cc on 16/3/19.
 */
public interface UpDownloadService {

    @GET
    Observable<ResponseBody> download(@Url String url);

}
