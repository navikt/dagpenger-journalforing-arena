#!/usr/bin/env bash

if test -f /secrets/serviceuser/srvdp-jrnf-arena/username;
then
    export  SRVDAGPENGER_JOURNALFORING_ARENA_USERNAME=$(cat /secrets/serviceuser/srvdp-jrnf-arena/username)
fi
if test -f /secrets/serviceuser/srvdp-jrnf-arena/password;
then
    export  SRVDAGPENGER_JOURNALFORING_ARENA_PASSWORD=$(cat /secrets/serviceuser/srvdp-jrnf-arena/password)
fi


