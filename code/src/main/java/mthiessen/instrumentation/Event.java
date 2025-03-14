package mthiessen.instrumentation;

public enum Event {
  INIT,

  PRE_RMW_FORWARD,

  POST_RMW_FORWARD_RECEIVE,

  PRE_RMW_FORWARD_RESPONSE_SEND,

  POST_RMW_FORWARD_RESPONSE,

  PRE_READ_FORWARD,

  POST_READ_FORWARD_RECEIVE,

  POST_READ_FORWARD_RESPONSE,

  PRE_READ_FORWARD_RESPONSE_SEND,

  PRE_PREPARE_BROADCAST,

  POST_PREPARE_BROADCAST_RECEIVE,

  PRE_PREPARE_RESPONSE_SEND,

  POST_PREPARE_RESPONSE_RECEIVED,

  POST_PREPARE_RESPONSE,

  POST_COMMIT_CAlL,

  PRE_LOCK_ACQUIRE,

  POST_LOCK_ACQUIRE,

  PRE_READ_WAIT,

  POST_READ_WAIT,

  POST_READ,

  READ_COMPLETE,

  POST_COMMIT_RECEIVE,

  POST_COMMIT_INITIATE,

  POST_COMMIT_SAFE_TO_APPLY,

  POST_RMW,

  POST_REFLECT_RMW_AS_COMMITTED,

  POST_DEL_COMMIT_WAIT,

  POST_DEL_COMMIT_BROADCAST,

  POST_PL_RMW_WAIT
}
