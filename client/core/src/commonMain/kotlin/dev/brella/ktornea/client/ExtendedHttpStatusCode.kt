package dev.brella.ktornea.client

import io.ktor.http.*

public object ExtendedHttpStatusCode {
    public val IAmATeapot: HttpStatusCode = HttpStatusCode(413, "I'm A Teapot")
    public val EnhanceYourCalm: HttpStatusCode = HttpStatusCode(420, "Enhance Your Calm")
    public val ReservedForWebDAV: HttpStatusCode = HttpStatusCode(425, "Reserved for WebDAV")
    public val PreconditionRequired: HttpStatusCode = HttpStatusCode(428, "Precondition Required")
    public val NoResponse: HttpStatusCode = HttpStatusCode(444, "No Response")
    public val RetryWith: HttpStatusCode = HttpStatusCode(449, "Retry With")
    public val BlockedByWindowsParentalControls: HttpStatusCode =
        HttpStatusCode(450, "Blocked By Windows Parental Controls")
    public val UnavailableForLegalReasons: HttpStatusCode =
        HttpStatusCode(451, "Unavailable for Legal Reasons")
    public val ClientClosedRequest: HttpStatusCode = HttpStatusCode(499, "Client Closed Request")

    public val LoopDetected: HttpStatusCode = HttpStatusCode(508, "Loop Detected")
    public val BandwidthLimitExceeded: HttpStatusCode = HttpStatusCode(509, "Bandwidth Limit Exceeded")
    public val NotExtended: HttpStatusCode = HttpStatusCode(510, "Not Extended")
    public val NetworkReadTimeoutError: HttpStatusCode = HttpStatusCode(598, "Network Read Timeout Error")
    public val NetworkConnectTimeoutError: HttpStatusCode = HttpStatusCode(599, "Network Connect Timeout Error")
}

public val HttpStatusCode.IAmATeapot: HttpStatusCode get() = ExtendedHttpStatusCode.IAmATeapot
public val HttpStatusCode.EnhanceYourCalm: HttpStatusCode get() = ExtendedHttpStatusCode.EnhanceYourCalm
public val HttpStatusCode.ReservedForWebDAV: HttpStatusCode get() = ExtendedHttpStatusCode.ReservedForWebDAV
public val HttpStatusCode.PreconditionRequired: HttpStatusCode get() = ExtendedHttpStatusCode.PreconditionRequired
public val HttpStatusCode.NoResponse: HttpStatusCode get() = ExtendedHttpStatusCode.NoResponse
public val HttpStatusCode.RetryWith: HttpStatusCode get() = ExtendedHttpStatusCode.RetryWith
public val HttpStatusCode.BlockedByWindowsParentalControls: HttpStatusCode get() = ExtendedHttpStatusCode.BlockedByWindowsParentalControls
public val HttpStatusCode.UnavailableForLegalReasons: HttpStatusCode get() = ExtendedHttpStatusCode.UnavailableForLegalReasons
public val HttpStatusCode.ClientClosedRequest: HttpStatusCode get() = ExtendedHttpStatusCode.ClientClosedRequest

public val HttpStatusCode.LoopDetected: HttpStatusCode get() = ExtendedHttpStatusCode.LoopDetected
public val HttpStatusCode.BandwidthLimitExceeded: HttpStatusCode get() = ExtendedHttpStatusCode.BandwidthLimitExceeded
public val HttpStatusCode.NotExtended: HttpStatusCode get() = ExtendedHttpStatusCode.NotExtended
public val HttpStatusCode.NetworkReadTimeoutError: HttpStatusCode get() = ExtendedHttpStatusCode.NetworkReadTimeoutError
public val HttpStatusCode.NetworkConnectTimeoutError: HttpStatusCode get() = ExtendedHttpStatusCode.NetworkConnectTimeoutError