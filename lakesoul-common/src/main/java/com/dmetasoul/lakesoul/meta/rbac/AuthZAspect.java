// SPDX-FileCopyrightText: 2023 LakeSoul Contributors
//
// SPDX-License-Identifier: Apache-2.0

package com.dmetasoul.lakesoul.meta.rbac;

import com.dmetasoul.lakesoul.meta.rbac.fetcher.AuthZFetcher;
import com.dmetasoul.lakesoul.meta.rbac.fetcher.AuthZNullFetcher;
import com.dmetasoul.lakesoul.meta.rbac.fetcher.AuthZParamFetcher;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.List;

@Aspect
public class AuthZAspect {

    AuthZAdvice advice;

    public AuthZAspect(){
        this.advice = new AuthZAdvice();
    }

    @Pointcut("execution(* *(..)) && @annotation(com.dmetasoul.lakesoul.meta.rbac.AuthZ)")
    public void pointcut(){

    }

    @Around("pointcut() && args(..)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        if(AuthZEnforcer.get() == null){
            // if authz is not enabled
            // proceed and return
            return joinPoint.proceed();
        }
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        AuthZ annotation = methodSignature.getMethod().getAnnotation(AuthZ.class);
        AuthZBefore authZBefore = methodSignature.getMethod().getAnnotation(AuthZBefore.class);
        AuthZAfter authZAfter = methodSignature.getMethod().getAnnotation(AuthZAfter.class);
        String object = annotation.object();
        String action = annotation.action();
        String value = annotation.value();
        if(value.contains(".")){
            String[] vals = annotation.value().split("\\.");
            object = vals[0];
            action = vals[1];
        }

        String subject = AuthZContext.getInstance().getSubject();
        String domain = AuthZContext.getInstance().getDomain();

        // if without fetcher
        if(authZAfter == null && authZBefore == null){
            if(!advice.hasPermit(subject, domain, object, action)){
                throw new AuthZException();
            }
        }

        // if has "before" fetcher
        if(authZBefore != null){
            Class<? extends AuthZFetcher> fetcher = authZBefore.fetcher();
            AuthZFetcher authZFetcher = null;
            if(fetcher != AuthZNullFetcher.class){
                authZFetcher = fetcher.newInstance();
            }else if(authZBefore != null) {
                authZFetcher = new AuthZParamFetcher(authZBefore);
            }

            List<String> objects = authZFetcher.getObject(joinPoint);
            validate(subject, domain, object, objects, action);
        }

        Object result = joinPoint.proceed();

        // if has "after" fetcher
        if(authZAfter != null){
            AuthZFetcher authZFetcher = authZAfter.fetcher().newInstance();
            List<String> objects = authZFetcher.getObject(result);
            validate(subject, domain, object, objects, action);
        }

        advice.after();
        return result;
    }


    private void validate(String subject, String domain, String baseObject, List<String> objects, String action){
        for(String obj : objects){
            // override domain and object passed from outside
            String objFullName = baseObject + "_" + obj;
            List<List<String>> policies = AuthZEnforcer.get().getFilteredNamedPolicy("p", 2, objFullName);
            if(policies.size() == 0) {
                throw new AuthZException();
            }
            String objDomain = policies.get(0).get(1);
            if(!advice.hasPermit(subject, objDomain, objFullName, action)){
                throw new AuthZException();
            }
        }

    }
}
